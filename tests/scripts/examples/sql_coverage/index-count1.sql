<grammar.sql>
INSERT INTO _table VALUES (0, NULL)
INSERT INTO _table VALUES (2, 2)
INSERT INTO _table VALUES (3, 3)
INSERT INTO _table VALUES (6, 6)
INSERT INTO _table VALUES (8, 8)
SELECT COUNT(*) FROM _table WHERE POINTS <  8 
SELECT COUNT(*) FROM _table WHERE POINTS <= 8
SELECT COUNT(*) FROM _table WHERE POINTS >  8
SELECT COUNT(*) FROM _table WHERE POINTS >= 8
SELECT COUNT(*) FROM _table WHERE POINTS =  8 
SELECT COUNT(*) FROM _table WHERE POINTS BETWEEN 1 and 7
SELECT COUNT(*) FROM _table WHERE POINTS BETWEEN 1 and 9
SELECT COUNT(*) FROM _table WHERE POINTS BETWEEN 3 and 7
SELECT COUNT(*) FROM _table WHERE POINTS BETWEEN 3 and 9

-- TODO: temp, for sqlCoverage testing:
SELECT * FROM _table

-- Causes NPE in HsqlDb (ENG-8292):
SELECT ID, (SELECT SUM(ID) FROM _table WHERE A2.ID = ID) FROM _table AS A2 GROUP BY ID HAVING SUM(ID) = 12

-- May cause NPE in HsqlDb (ENG-8292):
--SELECT ID, (SELECT SUM(ID) FROM _table WHERE A2.ID = ID) FROM _table AS A2 GROUP BY ID HAVING SUM(ID) = 12

-- May cause NPE in HsqlDb (ENG-8381):
--SELECT PKEY FROM A UNION ALL SELECT I FROM B UNION ALL SELECT I FROM C ORDER BY PKEY OFFSET 3

-- May cause NPE in HsqlDb (ENG-8273):
--select A.ID, count(*), (SELECT max(ID) FROM R2 B where B.count = A.count) FROM _table A GROUP BY ID, count order by ID, count