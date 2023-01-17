package dird

val domainTypesTableCreateSql = """
create table domain_tags
(
    id   integer
        primary key autoincrement,
    name text
        unique
);
"""
val domainTagsTableCreateSql = """
create table domain_types
(
    id   integer
        primary key,
    name text not null,
    check (id IN (0, 1, 2, 3))
);
"""
val domainsTableCreateSql = """
create table domains
(
    tag_id  integer not null
        constraint domains_domain_tags_id_fk
            references domain_tags
        constraint domains_domain_types_id_fk
            references domain_types,
    type_id integer not null,
    domain   text    not null
);
"""
val domainTagsInsertSqlTemplate = "insert into domain_tags (name) values (?);"
val domainsInsertSqlTemplate = """
insert into domains (tag_id, type_id, domain)
select domain_tags.id, ?, ? from domain_tags where domain_tags.name = ?
"""
val domainTypesInsertSql = """
insert into domain_types (id, name)
values  (0, 'full'),
        (1, 'suffix'),
        (2, 'keyword'),
        (3, 'regex');"""

val ipSetTagsTableCreateSql = """
create table ip_set_tags
(
    id   integer
        primary key autoincrement,
    name text
        unique
);
"""

val ipSetTableCreateSql = """
create table ip_set
(
    tag_id  integer not null
        constraint ip_set_ip_set_tags_id_fk
            references ip_set_tags,
    cidr    blob    not null
);
"""
val ipSetTagsInsertSqlTemplate = "insert into ip_set_tags (name) values (?);"
val ipSetInsertSqlTemplate = """
insert into ip_set (tag_id, cidr)
select ip_set_tags.id, ? from ip_set_tags where ip_set_tags.name = ?
"""
