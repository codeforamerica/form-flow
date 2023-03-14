-- This will make the original id a column a uuid column
alter table submissions add column new_id uuid UNIQUE DEFAULT gen_random_uuid();
update submissions set new_id = gen_random_uuid() where new_id is null;
alter table user_files add column new_submission_id uuid;

update user_files uf set new_submission_id = s.new_id from submissions s where s.id = uf.submission_id;
alter table user_files drop constraint user_files_submission_id_fkey;

alter table submissions drop column id;
alter table submissions rename column new_id to id;

alter table user_files drop column submission_id;
alter table user_files rename column new_submission_id to submission_id;
alter table user_files add constraint user_files_submission_id_fkey FOREIGN KEY (submission_id) REFERENCES submissions (id);



