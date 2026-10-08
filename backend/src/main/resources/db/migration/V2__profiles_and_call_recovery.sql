ALTER TABLE devices ADD COLUMN profile_image TEXT;
ALTER TABLE call_sessions ADD COLUMN offer_sdp TEXT;
ALTER TABLE call_sessions ADD COLUMN ice_candidates TEXT;
