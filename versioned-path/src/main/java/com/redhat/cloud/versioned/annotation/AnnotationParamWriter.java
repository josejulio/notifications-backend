package com.redhat.cloud.versioned.annotation;

import com.sun.codemodel.JAnnotatable;
import com.sun.codemodel.JAnnotationArrayMember;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JType;

import javax.ws.rs.POST;

public class AnnotationParamWriter {

    private String key;
    private JAnnotationUse jAnnotationUse;
    private JAnnotationArrayMember jAnnotationArrayMember;

    public AnnotationParamWriter(String key, JAnnotationUse jAnnotationUse) {
        this.key = key;
        this.jAnnotationUse = jAnnotationUse;
        this.jAnnotationArrayMember = null;
    }

    private AnnotationParamWriter(String key, JAnnotationArrayMember jAnnotationArrayMember) {
        this.key = key;
        this.jAnnotationUse = null;
        this.jAnnotationArrayMember = jAnnotationArrayMember;
    }

    public AnnotationParamWriter writeArray() {
        return new AnnotationParamWriter(key, jAnnotationUse.paramArray(key));
    }

    public void writeString(String value) {
        if (jAnnotationUse != null) {
            jAnnotationUse.param(key, value);
        } else {
            jAnnotationArrayMember.param(value);
        }
    }

    public void writeJType(JType value) {
        if (jAnnotationUse != null) {
            jAnnotationUse.param(key, value);
        } else {
            jAnnotationArrayMember.annotate((JClass) value);
        }
    }

    public JAnnotationUse writeClass(Class<? extends java.lang.annotation.Annotation> value) {
        if (jAnnotationUse != null) {
            return jAnnotationUse.annotationParam(key, value);
        } else {
            return jAnnotationArrayMember.annotate(value);
        }
    }

    public void writeEnum(Enum<?> value) {
        if (jAnnotationUse != null) {
            jAnnotationUse.param(key, value);
        } else {
            jAnnotationArrayMember.param(value);
        }
    }

    public void writeInteger(int value) {
        if (jAnnotationUse != null) {
            jAnnotationUse.param(key, value);
        } else {
            jAnnotationArrayMember.param(value);
        }
    }

    public void writeLong(long value) {
        if (jAnnotationUse != null) {
            jAnnotationUse.param(key, value);
        } else {
            jAnnotationArrayMember.param(value);
        }
    }

    public void writeShort(short value) {
        if (jAnnotationUse != null) {
            jAnnotationUse.param(key, value);
        } else {
            jAnnotationArrayMember.param(value);
        }
    }

    public void writeByte(byte value) {
        if (jAnnotationUse != null) {
            jAnnotationUse.param(key, value);
        } else {
            jAnnotationArrayMember.param(value);
        }
    }

    public void writeFloat(float value) {
        if (jAnnotationUse != null) {
            jAnnotationUse.param(key, value);
        } else {
            jAnnotationArrayMember.param(value);
        }
    }

    public void writeDouble(double value) {
        if (jAnnotationUse != null) {
            jAnnotationUse.param(key, value);
        } else {
            jAnnotationArrayMember.param(value);
        }
    }
}
