ALTER TABLE auto_responder
    ADD COLUMN enabled bool NOT NULL default FALSE;

UPDATE auto_responder
SET enabled=coalesce(slot_query.enabled, FALSE)
FROM (SELECT enabled, slot_id from guild_slot) as slot_query
WHERE auto_responder.slot_id = slot_query.slot_id;

ALTER TABLE guild_slot DROP COLUMN enabled;
