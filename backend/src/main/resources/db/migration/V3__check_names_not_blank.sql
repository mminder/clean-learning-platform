-- An offering's name and an expert's display name are never blank: they hold at least one character that isn't
-- whitespace. The domain's OfferingName and DisplayName have the same rule.

alter table offering
    add constraint offering_name_not_blank check (name ~ '\S');

alter table expert_profile
    add constraint expert_profile_display_name_not_blank check (display_name ~ '\S');
