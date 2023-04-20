alter table submissions
    add primary key (id);
alter table submissions
    alter column id set not null;
