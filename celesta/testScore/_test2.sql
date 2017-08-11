CREATE GRAIN test2 VERSION '2.0';

-- *** TABLES ***
CREATE TABLE c(
  iii INT NOT NULL IDENTITY,
  bbb VARCHAR(2),
  sss INT,
  CONSTRAINT pk_c PRIMARY KEY (iii)
);

CREATE TABLE d(
  e VARCHAR(5) NOT NULL DEFAULT '-',
  CONSTRAINT pk_d PRIMARY KEY (e)
);

CREATE TABLE a(
  a INT NOT NULL DEFAULT 0,
  /**a celestadoc*/
  b VARCHAR(5) NOT NULL DEFAULT '',
  c DATETIME,
  d INT,
  kk VARCHAR(5),
  CONSTRAINT pk_a PRIMARY KEY (a, b)
);

CREATE TABLE b(
  a VARCHAR(5) NOT NULL DEFAULT '',
  b INT,
  c INT,
  CONSTRAINT pk_b PRIMARY KEY (a)
);

-- *** FOREIGN KEYS ***
ALTER TABLE a ADD CONSTRAINT fk_test2_a_test2_d_kk FOREIGN KEY (kk) REFERENCES test2.d(e) ON UPDATE SET NULL;
ALTER TABLE a ADD CONSTRAINT fk_test2_a_test2_c_d FOREIGN KEY (d) REFERENCES test2.c(iii);
ALTER TABLE b ADD CONSTRAINT fk_test2_b_test2_a_b FOREIGN KEY (b, a) REFERENCES test2.a(a, b) ON UPDATE CASCADE ON DELETE CASCADE;
-- *** INDICES ***
-- *** VIEWS ***
-- *** MATERIALIZED VIEWS ***
-- *** PARAMETERIZED VIEWS ***
