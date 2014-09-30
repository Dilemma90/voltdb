/* This file is part of VoltDB.
 * Copyright (C) 2008-2014 VoltDB Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */
#ifndef STREAMBLOCK_H_
#define STREAMBLOCK_H_

#include "common/FatalException.hpp"

#include <cassert>
#include <cstring>
#include <stdint.h>
#include <limits>

#define MAGIC_HEADER_SPACE_FOR_JAVA 8
namespace voltdb
{
    enum StreamBlockType {
        NORMAL_STREAM_BLOCK = 1,
        LARGE_STREAM_BLOCK = 2
    };
    /**
     * A single data block with some buffer semantics.
     */
    class StreamBlock {
    public:
        StreamBlock(char* data, size_t capacity, size_t uso)
            : m_data(data + MAGIC_HEADER_SPACE_FOR_JAVA), m_capacity(capacity - MAGIC_HEADER_SPACE_FOR_JAVA), m_offset(0),
              m_uso(uso),
              m_startSpHandle(std::numeric_limits<int64_t>::max()),
              m_lastSpHandle(std::numeric_limits<int64_t>::max()),
              m_lastCommittedSpHandle(std::numeric_limits<int64_t>::max()),
              m_startSpUniqueId(std::numeric_limits<int64_t>::max()),
              m_lastSpUniqueId(std::numeric_limits<int64_t>::max()),
              m_lastDRBeginTxnOffset(0),
              m_hasDRBeginTxn(false),
              m_type(voltdb::NORMAL_STREAM_BLOCK)
        {
        }

        StreamBlock(StreamBlock *other)
            : m_data(other->m_data), m_capacity(other->m_capacity), m_offset(other->m_offset),
              m_uso(other->m_uso),
              m_startSpHandle(std::numeric_limits<int64_t>::max()),
              m_lastSpHandle(std::numeric_limits<int64_t>::max()),
              m_lastCommittedSpHandle(std::numeric_limits<int64_t>::max()),
              m_startSpUniqueId(other->m_startSpUniqueId),
              m_lastSpUniqueId(other->m_lastSpUniqueId),
              m_lastDRBeginTxnOffset(other->m_lastDRBeginTxnOffset),
              m_hasDRBeginTxn(other->m_hasDRBeginTxn),
              m_type(other->m_type)
        {
        }

        ~StreamBlock()
        {
        }

        /**
         * Returns a pointer to the underlying raw memory allocation
         */
        char* rawPtr() {
            return m_data - MAGIC_HEADER_SPACE_FOR_JAVA;
        }

        int32_t rawLength() const {
            return  static_cast<int32_t>(m_offset) + MAGIC_HEADER_SPACE_FOR_JAVA;
        }

        /**
         * Returns the universal stream offset of the block not
         * including any of the octets in this block.
         */
        size_t uso() const {
            return m_uso;
        }

        /**
         * Returns the additional offset from uso() to count all the
         * octets in this block.  uso() + offset() will compute the
         * universal stream offset for the entire block. This excludes
         * the length prefix.
         */
        size_t offset() const {
            return m_offset;
        }

        /**
         * Number of bytes left in the buffer
         */
        size_t remaining() const {
            return m_capacity - m_offset;
        }

        int64_t startSpUniqueId() {
            return m_startSpUniqueId;
        }

        /*
         * Sneakily set both start and last
         */
        void startSpUniqueId(int64_t spUniqueId) {
            m_lastSpUniqueId = spUniqueId;
            m_startSpUniqueId = std::min(spUniqueId, m_startSpUniqueId);
        }

        int64_t lastSpUniqueId() {
            return m_lastSpUniqueId;
        }

        void lastSpUniqueId(int64_t spUniqueId) {
            m_lastSpUniqueId = spUniqueId;
        }

        /**
         * Number of maximum bytes stored in the buffer
         */
        size_t capacity() const {
            return m_capacity;
        }

        size_t lastDRBeginTxnOffset() const {
            return m_lastDRBeginTxnOffset;
        }

        StreamBlockType type() const {
            return m_type;
        }

    private:
        char* mutableDataPtr() {
            return m_data + m_offset;
        }

        void consumed(size_t consumed) {
            assert ((m_offset + consumed) <= m_capacity);
            m_offset += consumed;
        }

        void truncateTo(size_t mark) {
            // just move offset. pretty easy.
            if (((m_uso + offset()) >= mark ) && (m_uso <= mark)) {
                m_offset = mark - m_uso;
            }
            else {
                throwFatalException("Attempted Export block truncation past start of block."
                                    "\n m_uso(%jd), m_offset(%jd), mark(%jd)\n",
                                    (intmax_t)m_uso, (intmax_t)m_offset, (intmax_t)mark);
            }
        }

        void recordLastBeginTxnOffset() {
            m_lastDRBeginTxnOffset = m_offset;
            m_hasDRBeginTxn = true;
        }

        void clearLastBeginTxnOffset() {
            m_lastDRBeginTxnOffset = 0;
            m_hasDRBeginTxn =false;
        }

        bool hasDRBeginTxn() {
            return m_hasDRBeginTxn;
        }

        char* mutableLastBeginTxnDataPtr() {
            return m_data + m_lastDRBeginTxnOffset;
        }

        void setType(StreamBlockType type) { m_type = type; }

        char *m_data;
        const size_t m_capacity;
        size_t m_offset;         // position for next write.
        size_t m_uso;            // universal stream offset of m_offset 0.
        int64_t m_startSpHandle;
        int64_t m_lastSpHandle;
        int64_t m_lastCommittedSpHandle;
        int64_t m_startSpUniqueId;
        int64_t m_lastSpUniqueId;
        size_t m_lastDRBeginTxnOffset;  // keep record of DR begin txn to avoid txn span multiple buffers
        bool m_hasDRBeginTxn;    // only used for DR Buffer
        StreamBlockType m_type;

        friend class TupleStreamBase;
        friend class ExportTupleStream;
        friend class DRTupleStream;
    };
}

#endif