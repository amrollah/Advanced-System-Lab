#!/bin/sh
psql -U postgres -d asl -h localhost -f  DB_scheme.sql