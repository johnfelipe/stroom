/*
 * This file is generated by jOOQ.
 */
package stroom.config.impl.db.jooq;


import javax.annotation.Generated;

import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.Internal;

import stroom.config.impl.db.jooq.tables.Config;


/**
 * A class modelling indexes of tables of the <code>stroom</code> schema.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.9"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index CONFIG_NAME = Indexes0.CONFIG_NAME;
    public static final Index CONFIG_PRIMARY = Indexes0.CONFIG_PRIMARY;

    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class Indexes0 {
        public static Index CONFIG_NAME = Internal.createIndex("name", Config.CONFIG, new OrderField[] { Config.CONFIG.NAME }, true);
        public static Index CONFIG_PRIMARY = Internal.createIndex("PRIMARY", Config.CONFIG, new OrderField[] { Config.CONFIG.ID }, true);
    }
}
