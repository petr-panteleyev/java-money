/*
 * Copyright (c) 2017, Petr Panteleyev <petr@panteleyev.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.panteleyev.money.persistence;

import org.panteleyev.persistence.annotations.Field;
import org.panteleyev.persistence.annotations.RecordBuilder;
import org.panteleyev.persistence.annotations.Table;
import java.util.Objects;

@Table("category")
public final class Category implements MoneyRecord, Named {
    private final int id;
    private final String name;
    private final String comment;
    private final int catTypeId;
    private final boolean expanded;
    private final String guid;
    private final long modified;

    private final CategoryType type;

    @RecordBuilder
    public Category(@Field("id") int id,
                    @Field("name") String name,
                    @Field("comment") String comment,
                    @Field("type_id") int catTypeId,
                    @Field("expanded") boolean expanded,
                    @Field("guid") String guid,
                    @Field("modified") long modified) {
        this.id = id;
        this.name = name;
        this.comment = comment;
        this.catTypeId = catTypeId;
        this.expanded = expanded;
        this.guid = guid;
        this.modified = modified;

        this.type = CategoryType.get(this.catTypeId);
    }

    public Category copy(String newName, String newComment, int newCatTypeId) {
        return new Category(id, newName, newComment, newCatTypeId, expanded, guid, System.currentTimeMillis());
    }

    public final CategoryType getType() {
        return type;
    }

    public final Category expand(boolean exp) {
        return new Category(id, name, comment, catTypeId, exp, guid, modified);
    }

    public Category copy(int newId) {
        return new Category(newId, name, comment, catTypeId, expanded, guid, modified);
    }

    @Override
    @Field(value = "id", primaryKey = true)
    public int getId() {
        return id;
    }

    @Override
    @Field("name")
    public String getName() {
        return name;
    }

    @Field("comment")
    public final String getComment() {
        return comment;
    }

    @Field("type_id")
    public final int getCatTypeId() {
        return catTypeId;
    }

    @Field("expanded")
    public final boolean getExpanded() {
        return expanded;
    }

    @Override
    @Field("guid")
    public String getGuid() {
        return guid;
    }

    @Override
    @Field("modified")
    public long getModified() {
        return modified;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, comment, catTypeId, expanded, guid, modified);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Category)) {
            return false;
        }

        Category that = (Category) other;

        return this.id == that.id
                && Objects.equals(this.name, that.name)
                && Objects.equals(this.comment, that.comment)
                && this.catTypeId == that.catTypeId
                && this.expanded == that.expanded
                && Objects.equals(this.guid, that.guid)
                && this.modified == that.modified;
    }
}
