-- The expert's photo (UC-14): the second owner of an image after the offering (D-21), null without a photo. An image
-- belongs to one owner at most, so replacing or removing the photo may delete it. The unique constraint's index also
-- serves the foreign key check when an image is deleted.

alter table expert_profile
    add column photo_id uuid unique references image (id);
