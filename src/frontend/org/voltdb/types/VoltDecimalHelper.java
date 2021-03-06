/* This file is part of VoltDB.
 * Copyright (C) 2008-2015 VoltDB Inc.
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

package org.voltdb.types;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A class for serializing and deserializing Volt's 16-byte fixed precision and scale decimal format. The decimal's
 * are converted to/from Java's {@link java.math.BigDecimal BigDecimal} class. <code>BigDecimal</code> stores values
 * as an unscaled (unscaled means no trailing 0s) fixed point {@link java.math.BigInteger BigInteger} and a separate
 * scale value. An exception (either {@link java.lang.RuntimeException RuntimeException} or
 * {@link java.io.IOException IOException}) if a <code>BigDecimal</code> with a scale > 12 or precision greater then
 * 38 is used. {@link java.math.BigDecimal#setScale(int) BigDecimal.setScale(int)} can be used to reduce the scale of
 * a value before serialization.
 *
 */
public class VoltDecimalHelper {
    /**
     * The scale of decimals in Volt
     */
    public static final int kDefaultScale = 12;

    /**
     * The precision of decimals in Volt
     */
    public static final int kDefaultPrecision = 38;

    /**
     * Array containing the smallest 16-byte twos complement value that is used
     * as SQL null.
     */
    private static final byte[] NULL_INDICATOR =
        new BigInteger("-170141183460469231731687303715884105728").toByteArray();

    /**
     * Math context specifying the precision of decimals in Volt.
     */
    private static final MathContext context = new MathContext( kDefaultPrecision );

    /**
     * Array of scale factors used to scale up <code>BigInteger</code>s retrieved from
     * <code>BigDecimal</code>s
     */
    private static final BigInteger scaleFactors[] = new BigInteger[] {
        BigInteger.ONE,
        BigInteger.TEN,
        BigInteger.TEN.pow(2),
        BigInteger.TEN.pow(3),
        BigInteger.TEN.pow(4),
        BigInteger.TEN.pow(5),
        BigInteger.TEN.pow(6),
        BigInteger.TEN.pow(7),
        BigInteger.TEN.pow(8),
        BigInteger.TEN.pow(9),
        BigInteger.TEN.pow(10),
        BigInteger.TEN.pow(11),
        BigInteger.TEN.pow(12)
    };

    private final static String m_roundingEnabledProperty = "BIGDECIMAL_ROUND";

    private final static String m_defaultRoundingEnablement = "true";

    private final static String m_roundingModeProperty = "BIGDECIMAL_ROUND_POLICY";

    private final static String m_defaultRoundingMode = "HALF_UP";

    /*
     * This is the class of rounding configurations.  This is really
     * a pair, dressed up in glad rags.  Note that the only way to set
     * this is to set both components.
     */
    private static class RoundingConfiguration {
        private RoundingMode m_roundingMode;
        private boolean m_roundingIsEnabled;
        public RoundingConfiguration(boolean enabled, RoundingMode mode) {
            m_roundingIsEnabled = enabled;
            m_roundingMode = mode;
        }
        public final RoundingMode getRoundingMode() {
            return m_roundingMode;
        }
        public final Boolean getRoundingIsEnabled() {
            return m_roundingIsEnabled;
        }
        public final void setConfig(boolean enabled, RoundingMode roundingMode) {
            m_roundingIsEnabled = enabled;
            m_roundingMode = roundingMode;
        }
    };

    private static RoundingConfiguration m_roundingConfiguration
        = new RoundingConfiguration(Boolean.valueOf(System.getProperty(m_roundingEnabledProperty, m_defaultRoundingEnablement)),
                                    RoundingMode.valueOf(System.getProperty(m_roundingModeProperty, m_defaultRoundingMode)));


    static public byte[] getUnscaledBytes(BigDecimal bd) throws IOException {
        if (bd == null) {
            return Arrays.copyOf(NULL_INDICATOR, NULL_INDICATOR.length);
        }
        if (bd.scale() > kDefaultScale) {
            bd = roundToScale(bd, kDefaultScale, getRoundingMode());
        }
        int scale = bd.scale();
        int precision = bd.precision();
        final int precisionMinusScale = precision - scale;
        if ( precisionMinusScale > 26 ) {
            throw new IOException("Precision of " + bd + " to the left of the decimal point is " +
                    precisionMinusScale + " and the max is 26");
        }
        final int scaleFactor = kDefaultScale - bd.scale();
        BigInteger unscaledBI = bd.unscaledValue().multiply(scaleFactors[scaleFactor]);
        boolean isNegative = false;
        if (unscaledBI.signum() < 0) {
            isNegative = true;
        }
        final byte unscaledValue[] = unscaledBI.toByteArray();
        if (unscaledValue.length > 16) {
            throw new IOException("Precision of " + bd + " is >38 digits");
        }
        return expandToLength16(unscaledValue, isNegative);
    }

    /**
     * Serialize the null decimal sigil to a the provided {@link java.nio.ByteBuffer ByteBuffer}
     * @param buf <code>ByteBuffer</code> to serialize the decimal into
     */
    static public void serializeNull(ByteBuffer buf) {
        buf.put(NULL_INDICATOR);
    }

    /**
     * Converts BigInteger's byte representation containing a scaled magnitude to a fixed size 16 byte array
     * and set the sign in the most significant byte's most significant bit.
     * @param scaledValue Scaled twos complement representation of the decimal
     * @param isNegative Determines whether the sign bit is set
     * @return
     */
    private static final byte[] expandToLength16(byte scaledValue[], final boolean isNegative) {
        if (scaledValue.length == 16) {
            return scaledValue;
        }
        byte replacement[] = new byte[16];
        if (isNegative){
            java.util.Arrays.fill( replacement, (byte)-1);
        }
        for (int ii = 15; 15 - ii < scaledValue.length; ii--) {
            replacement[ii] = scaledValue[ii - (replacement.length - scaledValue.length)];
        }
        return replacement;
    }

    static public byte[] serializeBigDecimal(BigDecimal bd) {
        ByteBuffer buf = ByteBuffer.allocate(16);
        serializeBigDecimal(bd, buf);
        return buf.array();
    }

    public static final boolean isRoundingEnabled() {
        return m_roundingConfiguration.getRoundingIsEnabled();
    }

    public static synchronized final void setRoundingConfig(boolean enabled, RoundingMode mode) {
        m_roundingConfiguration.setConfig(enabled, mode);
    }

    public static final RoundingMode getRoundingMode() {
        return m_roundingConfiguration.getRoundingMode();
    }

    /**
     * Round a BigDecimal number to a scale given the rounding mode.
     * Note that rounding may return the precision.  For example,
     * rounding 9.99999 and 9.1999 to a scale of 2 gives 10.00 and 9.20.
     * The latter has precision 3, and the former has precision 4.
     * @param bd
     * @param scale
     * @return
     */
    static private final BigDecimal roundToScale(BigDecimal bd, int scale, RoundingMode mode) throws RuntimeException
    {
        int lostScaleDigits = bd.scale() - scale;
        if (lostScaleDigits <= 0) {
            return bd;
        }
        if (!isRoundingEnabled()) {
            throw new RuntimeException(String.format("Decimal scale %d is greater than the maximum %d", bd.scale(), kDefaultScale));
        }
        int desiredPrecision = bd.precision() - lostScaleDigits;
        MathContext mc = new MathContext(desiredPrecision, mode);
        BigDecimal nbd = bd.round(mc);
        if (nbd.scale() != scale) {
            nbd = nbd.setScale(scale);
        }
        assert(nbd.scale() == scale);
        return nbd;
    }

    /**
     * Serialize the {@link java.math.BigDecimal BigDecimal} to Volt's fixed precision and scale 16-byte format.
     * @param bd {@link java.math.BigDecimal BigDecimal} to serialize
     * @param buf {@link java.nio.ByteBuffer ByteBuffer} to serialize the <code>BigDecimal</code> to
     * @throws RuntimeException Thrown if the precision is out of range, or the scale is out of range and rounding is not enabled.
     */
    static public void serializeBigDecimal(BigDecimal bd, ByteBuffer buf)
    {
          if (bd == null) {
              serializeNull(buf);
              return;
          }
          if (bd.scale() > 12) {
              bd = roundToScale(bd, kDefaultScale, getRoundingMode());
          }
          int scale = bd.scale();
          int precision = bd.precision();
          final int precisionMinusScale = precision - scale;
          if ( precisionMinusScale > 26) {
              throw new RuntimeException("Precision of " + bd + " to the left of the decimal point is " +
                      precisionMinusScale + " and the max is 26");
          }
          final int scaleFactor = kDefaultScale - bd.scale();
          BigInteger unscaledBI = bd.unscaledValue().multiply(scaleFactors[scaleFactor]);
          boolean isNegative = false;
          if (unscaledBI.signum() < 0) {
              isNegative = true;
          }
          final byte unscaledValue[] = unscaledBI.toByteArray();
          if (unscaledValue.length > 16) {
              throw new RuntimeException("Precision of " + bd + " is >38 digits");
          }
          buf.put(expandToLength16(unscaledValue, isNegative));
    }

    /**
     * Deserialize a Volt fixed precision and scale 16-byte decimal from a String representation
     * @param decimal <code>String</code> representation of the decimal
     */
    public static BigDecimal deserializeBigDecimalFromString(String decimal) throws IOException
    {
        if (decimal == null) {
            return null;
        }
        BigDecimal bd = new BigDecimal(decimal);
        // if the scale is too large, check for trailing zeros
        if (bd.scale() > kDefaultScale) {
            bd = bd.stripTrailingZeros();
            if (bd.scale() > kDefaultScale) {
                bd = roundToScale(bd, kDefaultScale, getRoundingMode());
            }
        }
        // enforce scale 12 to make the precision check right
        if (bd.scale() < kDefaultScale) {
            bd = bd.setScale(kDefaultScale);
        }
        if (bd.precision() > 38) {
            throw new RuntimeException(
                    "Decimal " + bd + " has more than " + kDefaultPrecision + " digits of precision.");
        }
        return bd;
    }

    /**
     * Deserialize a Volt fixed precision and scale 16-byte decimal and return
     * it as a {@link java.math.BigDecimal BigDecimal} .
     * @param buffer {@link java.nio.ByteBuffer ByteBuffer} to read from
     */
    public static BigDecimal deserializeBigDecimal(ByteBuffer buffer) {
        byte decimalBytes[] = new byte[16];
        buffer.get(decimalBytes);
        if (java.util.Arrays.equals(decimalBytes, NULL_INDICATOR)) {
            return null;
        }
        final BigDecimal bd = new BigDecimal(
                new BigInteger(decimalBytes),
                        kDefaultScale, context);
        if (bd.precision() > 38) {
            throw new RuntimeException("Decimal " + bd + " has more than 38 digits of precision.");
        }
        return bd;
    }

    public static BigDecimal setDefaultScale(BigDecimal bd) {
        // TODO Auto-generated method stub
        return bd.setScale(kDefaultScale, getRoundingMode());
    }
}
