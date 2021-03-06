package stroom.security.shared;

import java.util.Objects;

/**
 * TODO: Rename this first opportunity, for now it's to distinguish it from the hibernate based user.
 */
public class User {
    public static final String ADMIN_USER_NAME = "admin";

    // Value of a long to represent an undefined id.
    private static final long UNDEFINED_ID = -1;

    /**
     * This is a technical key (the primary key) You should consider your own
     * business key (e.g. string name) if required. You should not reference
     * this key in the application code or in any screen.
     */
    private long id = UNDEFINED_ID;

    private String name;

    private String uuid;

    /**
     * Is this user a user group or a regular user? TODO : At some point split
     * out logon and credential details into another entity.
     */
    private boolean isGroup;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean getIsGroup() {
        return isGroup;
    }

    public void setIsGroup(boolean isGroup) {
        this.isGroup = isGroup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id &&
                isGroup == user.isGroup &&
                Objects.equals(name, user.name) &&
                Objects.equals(uuid, user.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, uuid, isGroup);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("User{");
        sb.append("id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", uuid='").append(uuid).append('\'');
        sb.append(", isGroup=").append(isGroup);
        sb.append('}');
        return sb.toString();
    }

    public static class Builder {
        private final User instance;

        public Builder(final User instance) {
            this.instance = instance;
        }

        public Builder() {
            this(new User());
        }

        public Builder id(final long value) {
            instance.setId(value);
            return this;
        }

        public Builder name(final String value) {
            instance.setName(value);
            return this;
        }

        public Builder uuid(final String value) {
            instance.setUuid(value);
            return this;
        }

        public Builder isGroup(final Boolean value) {
            instance.setIsGroup(value);
            return this;
        }

        public User build() {
            return this.instance;
        }
    }
}
