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
    domains text    not null
);
"""
val domainTagsInsertSqlTemplate = "insert into domain_tags (name) values (?);"
val domainTypesInsertSql = """
insert into domain_types (id, name)
values (0, 'full'),
       (1, 'suffix'),
       (2, 'keyword'),
       (3, 'regex');
"""
val domainsInsertSqlTemplate = """
insert into domains (tag_id, type_id, domains)
select domain_tags.id, ?, ? from domain_tags where domain_tags.name = ?
"""

val ipSetTagsTableCreateSql = """
create table ip_set_tags
(
    id   integer
        primary key autoincrement,
    name text
        unique
);
"""
val ipSetTypesTableCreateSql = """
create table ip_set_types
(
    id   integer
        primary key,
    name text not null,
    check (id IN (0, 1))
);
"""
val ipSetTableCreateSql = """
create table ip_sets
(
    tag_id  integer not null
        constraint ip_sets_ip_set_tags_id_fk
            references ip_set_tags
        constraint ip_sets_ip_set_types_id_fk
            references ip_set_types,
    type_id integer not null,
    cidrs   blob    not null
);
"""
val ipSetTagsInsertSqlTemplate = "insert into ip_set_tags (name) values (?);"
val ipSetTypesInsertSql = """
insert into ip_set_types (id, name)
values (0, 'IPv4 CIDRs'),
       (1, 'IPv6 CIDRs');
"""
val ipSetInsertSqlTemplate = """
insert into ip_sets (tag_id, type_id, cidrs)
select ip_set_tags.id, ?, ? from ip_set_tags where ip_set_tags.name = ?
"""
