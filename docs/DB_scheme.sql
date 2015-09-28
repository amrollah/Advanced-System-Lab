CREATE DATABASE asl;
DROP FUNCTION count_messages();
DROP FUNCTION Queue_Exist(integer);
DROP FUNCTION Get_Message_By_Queue(receiver_id INT, queue_id INT);
DROP FUNCTION Get_Message_By_Sender(receiver_id INT, sender_id INT);
DROP TABLE IF EXISTS client, queue, message;

CREATE TABLE IF NOT EXISTS client (
	id serial primary key
);

CREATE TABLE IF NOT EXISTS queue (
	id serial primary key
);

CREATE TABLE IF NOT EXISTS message (
	id serial primary key,
	sender_id int references client(id) NOT NULL,
	receiver_id int references client(id) DEFAULT NULL,
	queue_id int references queue(id) NOT NULL,
	arrive_time timestamp DEFAULT current_timestamp,
	content varchar(3000) NOT NULL
);

/* indexes */

CREATE INDEX sender_idx ON message (sender_id);
CREATE INDEX queue_idx ON message (queue_id);
CREATE INDEX receiver_idx ON message (receiver_id);

/* Functions */

CREATE OR REPLACE FUNCTION Create_Client(IN client__id INT)
RETURNS INT AS $body$
BEGIN
    INSERT INTO client(id) values($1);
    RETURN client__id;
EXCEPTION
    WHEN unique_violation
    THEN RETURN client__id;
END;
$body$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION Create_Queue(OUT id   INT)
RETURNS INT
AS 'INSERT INTO queue values(default) RETURNING id'
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION Delete_Queue(id INT = NULL)
RETURNS INT
AS 'DELETE FROM queue WHERE id=$1 RETURNING id'
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION Insert_Message(sender_id INT, receiver_id INT, queue_id INT, content varchar(2000))
RETURNS INT
AS 'INSERT INTO message(sender_id, receiver_id, queue_id, content) values($1,$2,$3,$4) RETURNING id'
LANGUAGE SQL;

CREATE TYPE msg_result AS (id INT, sender_id INT, receiver_id INT, queue_id INT, content varchar, arrive_time timestamp);

CREATE OR REPLACE FUNCTION Read_Message_By_Queue(receiver_id INT, queue_id INT)
RETURNS msg_result
AS 'SELECT id,sender_id,receiver_id,queue_id,content,arrive_time FROM message WHERE queue_id=$2 AND (receiver_id IS NULL OR receiver_id=$1) ORDER BY arrive_time DESC LIMIT 1'
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION Read_Message_By_Sender(receiver_id INT, sender_id INT)
RETURNS msg_result
AS 'SELECT id,sender_id,receiver_id,queue_id,content,arrive_time FROM message WHERE sender_id=$2 AND (receiver_id IS NULL OR receiver_id=$1) ORDER BY arrive_time DESC LIMIT 1'
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION Get_Message_By_Queue(receiver_id INT, queue_id INT)
RETURNS msg_result
AS 'DELETE FROM message WHERE id IN (SELECT id FROM message WHERE queue_id=$2 AND (receiver_id IS NULL OR receiver_id=$1) ORDER BY arrive_time DESC LIMIT 1) RETURNING id,sender_id,receiver_id,queue_id,content,arrive_time'
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION Get_Message_By_Sender(receiver_id INT, sender_id INT)
RETURNS msg_result
AS 'DELETE FROM message WHERE id IN (SELECT id FROM message WHERE sender_id=$2 AND (receiver_id IS NULL OR receiver_id=$1) ORDER BY arrive_time DESC LIMIT 1) RETURNING id,sender_id,receiver_id,queue_id,content,arrive_time'
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION Delete_Message(id INT = NULL)
RETURNS INT
AS 'DELETE FROM message WHERE id=$1 RETURNING id'
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION List_Pending_Queues(IN id INT)
RETURNS TABLE(
	queue_id INT
)
AS 'SELECT DISTINCT queue_id FROM message WHERE receiver_id IS NULL or receiver_id=$1'
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION List_Queues()
RETURNS TABLE(
	id INT
)
AS 'SELECT id FROM queue'
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION List_Clients()
RETURNS TABLE(
	id INT
)
AS 'SELECT id FROM client'
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION Count_Messages()
RETURNS bigint
AS 'SELECT COUNT (*) FROM message'
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION Queue_Exist(IN q_id INT )
RETURNS boolean
AS 'SELECT EXISTS(SELECT 1 FROM queue WHERE id=$1)'
LANGUAGE SQL;