--comment: create the assignments schema and lock down default access at the database layer
CREATE SCHEMA IF NOT EXISTS assignments;

REVOKE ALL ON SCHEMA assignments FROM PUBLIC;