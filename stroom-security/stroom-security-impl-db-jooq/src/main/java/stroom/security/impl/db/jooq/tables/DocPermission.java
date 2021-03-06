/*
 * This file is generated by jOOQ.
 */
package stroom.security.impl.db.jooq.tables;


import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

import stroom.security.impl.db.jooq.Indexes;
import stroom.security.impl.db.jooq.Keys;
import stroom.security.impl.db.jooq.Stroom;
import stroom.security.impl.db.jooq.tables.records.DocPermissionRecord;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.9"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DocPermission extends TableImpl<DocPermissionRecord> {

    private static final long serialVersionUID = 1673110760;

    /**
     * The reference instance of <code>stroom.doc_permission</code>
     */
    public static final DocPermission DOC_PERMISSION = new DocPermission();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<DocPermissionRecord> getRecordType() {
        return DocPermissionRecord.class;
    }

    /**
     * The column <code>stroom.doc_permission.id</code>.
     */
    public final TableField<DocPermissionRecord, Long> ID = createField("id", org.jooq.impl.SQLDataType.BIGINT.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.doc_permission.user_uuid</code>.
     */
    public final TableField<DocPermissionRecord, String> USER_UUID = createField("user_uuid", org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.doc_permission.doc_uuid</code>.
     */
    public final TableField<DocPermissionRecord, String> DOC_UUID = createField("doc_uuid", org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.doc_permission.permission</code>.
     */
    public final TableField<DocPermissionRecord, String> PERMISSION = createField("permission", org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * Create a <code>stroom.doc_permission</code> table reference
     */
    public DocPermission() {
        this(DSL.name("doc_permission"), null);
    }

    /**
     * Create an aliased <code>stroom.doc_permission</code> table reference
     */
    public DocPermission(String alias) {
        this(DSL.name(alias), DOC_PERMISSION);
    }

    /**
     * Create an aliased <code>stroom.doc_permission</code> table reference
     */
    public DocPermission(Name alias) {
        this(alias, DOC_PERMISSION);
    }

    private DocPermission(Name alias, Table<DocPermissionRecord> aliased) {
        this(alias, aliased, null);
    }

    private DocPermission(Name alias, Table<DocPermissionRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> DocPermission(Table<O> child, ForeignKey<O, DocPermissionRecord> key) {
        super(child, key, DOC_PERMISSION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Stroom.STROOM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.DOC_PERMISSION_PRIMARY, Indexes.DOC_PERMISSION_USER_UUID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity<DocPermissionRecord, Long> getIdentity() {
        return Keys.IDENTITY_DOC_PERMISSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<DocPermissionRecord> getPrimaryKey() {
        return Keys.KEY_DOC_PERMISSION_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<DocPermissionRecord>> getKeys() {
        return Arrays.<UniqueKey<DocPermissionRecord>>asList(Keys.KEY_DOC_PERMISSION_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ForeignKey<DocPermissionRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<DocPermissionRecord, ?>>asList(Keys.DOC_PERMISSION_IBFK_1);
    }

    public StroomUser stroomUser() {
        return new StroomUser(this, Keys.DOC_PERMISSION_IBFK_1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DocPermission as(String alias) {
        return new DocPermission(DSL.name(alias), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DocPermission as(Name alias) {
        return new DocPermission(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public DocPermission rename(String name) {
        return new DocPermission(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public DocPermission rename(Name name) {
        return new DocPermission(name, null);
    }
}
