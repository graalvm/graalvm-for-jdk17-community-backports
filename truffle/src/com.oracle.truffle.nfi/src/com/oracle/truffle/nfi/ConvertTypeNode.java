/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.nfi;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateAOT;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.nfi.NFIType.TypeCachedState;

//TODO GR-42818 fix warnings
@SuppressWarnings({"truffle-inlining", "truffle-sharing", "truffle-neverdefault", "truffle-limit"})
@GenerateAOT
abstract class ConvertTypeNode extends Node {

    abstract Object execute(NFIType type, Object value) throws UnsupportedTypeException;

    static final class OptimizedConvertTypeNode extends ConvertTypeNode {

        final TypeCachedState typeState;
        @Child ConvertTypeNode convertImpl;

        OptimizedConvertTypeNode(TypeCachedState typeState, ConvertTypeNode convertImpl) {
            this.typeState = typeState;
            this.convertImpl = convertImpl;
        }

        @Override
        Object execute(NFIType type, Object value) throws UnsupportedTypeException {
            assert type.cachedState == typeState;
            return convertImpl.execute(type, value);
        }
    }

    @GenerateUncached
    abstract static class ConvertToNativeNode extends ConvertTypeNode {

        @Specialization(guards = "type.cachedState == convertImpl.typeState", limit = "3")
        Object doCached(NFIType type, Object value,
                        @Cached("type.cachedState.createToNative()") OptimizedConvertTypeNode convertImpl) throws UnsupportedTypeException {
            return convertImpl.execute(type, value);
        }

        @Specialization(replaces = "doCached")
        Object doGeneric(NFIType type, Object value) throws UnsupportedTypeException {
            return type.cachedState.toNativeFactory.getUncachedInstance().execute(type, value);
        }
    }

    @GenerateUncached
    abstract static class ConvertFromNativeNode extends ConvertTypeNode {

        @Specialization(guards = "type.cachedState == convertImpl.typeState", limit = "3")
        Object doCached(NFIType type, Object value,
                        @Cached("type.cachedState.createFromNative()") OptimizedConvertTypeNode convertImpl) throws UnsupportedTypeException {
            return convertImpl.execute(type, value);
        }

        @Specialization(replaces = "doCached")
        Object doGeneric(NFIType type, Object value) throws UnsupportedTypeException {
            return type.cachedState.fromNativeFactory.getUncachedInstance().execute(type, value);
        }
    }
}
