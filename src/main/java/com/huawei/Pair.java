package com.huawei;

class Pair<K, V> {
    private K k;
    private V v;

    public Pair(K k, V v) {
        this.k = k;
        this.v = v;
    }

    public K getK() {
        return k;
    }

    public void setK(K k) {
        this.k = k;
    }

    public V getV() {
        return v;
    }

    public void setV(V v) {
        this.v = v;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof Pair))
            return false;
        Pair o = (Pair) obj;
        if ((k == null && ((Pair) obj).k != null) || (k != null && ((Pair) obj).k == null))
            return false;
        if ((v == null && ((Pair) obj).v != null) || (v != null && ((Pair) obj).v == null))
            return false;
        if (v != null && k != null)
            return (this.k.equals(o.k)) && (this.v.equals(o.v));
        else if (v == null && k != null)
            return this.k.equals(o.k);
        else
            return this.v.equals(o.v);
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (k != null)
            result = result * 31 + k.hashCode();
        if (v != null)
            result = result * 31 + v.hashCode();
        return result;
    }
}
