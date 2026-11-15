-- The expert's Teams link (UC-14): null without a link. The check repeats the domain's TeamsLink (D-36): https://
-- followed by at least one printable ASCII character, without spaces. Any host, not only Microsoft's (spec 06,
-- decision 8). Only the profile's own queries and the case details read it (spec 06, decision 15).

alter table expert_profile
    add column teams_link text check (teams_link ~ '^https://[!-~]+$');
