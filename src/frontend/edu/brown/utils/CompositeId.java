package edu.brown.utils;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.voltdb.catalog.Database;


public abstract class CompositeId implements Comparable<CompositeId>, JSONSerializable {
    
    private transient int hashCode = -1;
    
    protected final long encode(int...offset_bits) {
        long values[] = this.toArray();
        assert(values.length == offset_bits.length);
        long id = 0;
        int offset = 0;
        for (int i = 0; i < values.length; i++) {
            long max_value = (long)(Math.pow(2, offset_bits[i]) - 1l);

            assert(values[i] >= 0) :
                String.format("%s value at position %d is %d",
                              this.getClass().getSimpleName(), i, values[i]);
            assert(values[i] < max_value) :
                String.format("%s value at position %d is %d. Max value is %d",
                              this.getClass().getSimpleName(), i, values[i], max_value);
            
            id = (i == 0 ? values[i] : id | values[i]<<offset);
            offset += offset_bits[i];
        } // FOR
        this.hashCode = new Long(id).hashCode();
        return (id);
    }
    
    protected final long[] decode(long composite_id, int...offset_bits) {
        long values[] = new long[offset_bits.length];
        int offset = 0;
        for (int i = 0; i < values.length; i++) {
            long max_value = (long)(Math.pow(2, offset_bits[i]) - 1l);
            values[i] = (composite_id>>offset & max_value);
            offset += offset_bits[i];
        } // FOR
        return (values);
    }
    
    public abstract long encode();
    public abstract void decode(long composite_id);
    public abstract long[] toArray();
    
    @Override
    public int compareTo(CompositeId o) {
        return Math.abs(this.hashCode()) - Math.abs(o.hashCode());
    }
    
    @Override
    public int hashCode() {
        if (this.hashCode == -1) {
            this.encode();
            assert(this.hashCode != -1);
        }
        return (this.hashCode);
    }
    
    // -----------------------------------------------------------------
    // SERIALIZATION
    // -----------------------------------------------------------------
    
    @Override
    public void load(String input_path, Database catalog_db) throws IOException {
        JSONUtil.load(this, catalog_db, input_path);
    }
    @Override
    public void save(String output_path) throws IOException {
        JSONUtil.save(this, output_path);
    }
    @Override
    public String toJSONString() {
        return (JSONUtil.toJSONString(this));
    }
    @Override
    public void toJSON(JSONStringer stringer) throws JSONException {
        stringer.key("ID").value(this.encode());
    }
    @Override
    public void fromJSON(JSONObject json_object, Database catalog_db) throws JSONException {
        this.decode(json_object.getLong("ID"));
    }
}
