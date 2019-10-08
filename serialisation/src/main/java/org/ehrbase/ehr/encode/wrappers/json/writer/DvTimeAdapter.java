/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.ehr.encode.wrappers.json.writer;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvTime;
import org.ehrbase.ehr.encode.wrappers.ObjectSnakeCase;
import org.ehrbase.ehr.encode.wrappers.json.I_DvTypeAdapter;

import java.io.IOException;

/**
 * GSON adapter for DvDateTime
 * Required since JSON does not support natively a DateTime data type
 */
public class DvTimeAdapter extends DvTypeAdapter<DvTime> {

    public DvTimeAdapter(AdapterType adapterType) {
        super(adapterType);
    }

    public DvTimeAdapter() {
    }

    @Override
    public DvTime read(JsonReader arg0) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void write(JsonWriter writer, DvTime dvalue) throws IOException {
        if (dvalue == null) {
            writer.nullValue();
            return;
        }

        if (adapterType == AdapterType.PG_JSONB) {
            writer.beginObject();
            writer.name("value").value(dvalue.getValue().toString());
            writer.name("epoch_offset").value(dvalue.getValue().toString());
            writer.endObject();
        } else if (adapterType == AdapterType.RAW_JSON) {
            writer.beginObject();
            writer.name(I_DvTypeAdapter.TAG_CLASS_RAW_JSON).value(new ObjectSnakeCase(dvalue).camelToUpperSnake());
            writer.name("value").value(dvalue.getValue().toString());
            writer.endObject();

        }

    }

}
