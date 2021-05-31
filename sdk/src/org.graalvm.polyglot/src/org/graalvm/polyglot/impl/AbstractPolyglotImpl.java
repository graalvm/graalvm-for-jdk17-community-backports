/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.polyglot.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.AnnotatedElement;
import java.net.URI;
import java.net.URL;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.graalvm.collections.UnmodifiableEconomicSet;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.HostAccess.TargetMappingPrecedence;
import org.graalvm.polyglot.Instrument;
import org.graalvm.polyglot.Language;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.PolyglotException.StackFrame;
import org.graalvm.polyglot.ResourceLimitEvent;
import org.graalvm.polyglot.ResourceLimits;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.SourceSection;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.ByteSequence;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.MessageTransport;
import org.graalvm.polyglot.io.ProcessHandler;
import org.graalvm.polyglot.management.ExecutionEvent;

/**
 * This class is intended to be used by polyglot implementations. Methods in this class are not
 * intended to be used directly.
 *
 * This class and its inner classes break compatibility without notice. Do not use, unless you know
 * what you are doing.
 */
@SuppressWarnings("unused")
public abstract class AbstractPolyglotImpl {

    protected AbstractPolyglotImpl() {
    }

    public abstract static class ManagementAccess {
        protected ManagementAccess() {
            if (!getClass().getCanonicalName().equals("org.graalvm.polyglot.management.Management.ManagementAccessImpl")) {
                throw new AssertionError("Only one implementation of ManagementAccessImpl allowed. " + getClass().getCanonicalName());
            }
        }

        public abstract ExecutionEvent newExecutionEvent(Object event);

    }

    public abstract static class IOAccess {
        protected IOAccess() {
            if (!getClass().getCanonicalName().equals("org.graalvm.polyglot.io.IOHelper.IOAccessImpl")) {
                throw new AssertionError("Only one implementation of IOAccess allowed. " + getClass().getCanonicalName());
            }
        }

        public abstract ProcessHandler.ProcessCommand newProcessCommand(List<String> cmd, String cwd, Map<String, String> environment, boolean redirectErrorStream,
                        ProcessHandler.Redirect inputRedirect, ProcessHandler.Redirect outputRedirect, ProcessHandler.Redirect errorRedirect);

        public abstract ProcessHandler.Redirect createRedirectToStream(OutputStream stream);

        public abstract OutputStream getOutputStream(ProcessHandler.Redirect redirect);
    }

    public abstract static class APIAccess {

        protected APIAccess() {
            if (!getClass().getCanonicalName().equals("org.graalvm.polyglot.Engine.APIAccessImpl")) {
                throw new AssertionError("Only one implementation of APIAccess allowed. " + getClass().getCanonicalName());
            }
        }

        public abstract Engine newEngine(AbstractEngineDispatch dispatch, Object receiver);

        public abstract Context newContext(AbstractContextDispatch dispatch, Object receiver, Engine engine);

        public abstract Language newLanguage(AbstractLanguageDispatch dispatch, Object receiver);

        public abstract Instrument newInstrument(AbstractInstrumentDispatch dispatch, Object receiver);

        public abstract Value newValue(AbstractValueDispatch dispatch, Object receiver);

        public abstract Source newSource(Object receiver);

        public abstract SourceSection newSourceSection(Source source, Object receiver);

        public abstract PolyglotException newLanguageException(String message, AbstractExceptionDispatch dispatch, Object receiver);

        public abstract Object getReceiver(Instrument instrument);

        public abstract Object getReceiver(Engine engine);

        public abstract Object getReceiver(Context context);

        public abstract Object getReceiver(PolyglotException exception);

        public abstract Object getReceiver(Value value);

        public abstract Object getReceiver(ResourceLimits value);

        public abstract AbstractValueDispatch getDispatch(Value value);

        public abstract AbstractStackFrameImpl getDispatch(StackFrame value);

        public abstract AbstractLanguageDispatch getDispatch(Language value);

        public abstract AbstractInstrumentDispatch getDispatch(Instrument value);

        public abstract AbstractEngineDispatch getDispatch(Engine engine);

        public abstract AbstractContextDispatch getDispatch(Context context);

        public abstract ResourceLimitEvent newResourceLimitsEvent(Context context);

        public abstract StackFrame newPolyglotStackTraceElement(AbstractStackFrameImpl dispatch, Object receiver);

        public abstract List<Object> getTargetMappings(HostAccess access);

        public abstract boolean allowsAccess(HostAccess access, AnnotatedElement element);

        public abstract boolean allowsImplementation(HostAccess access, Class<?> type);

        public abstract boolean isArrayAccessible(HostAccess access);

        public abstract boolean isListAccessible(HostAccess access);

        public abstract boolean isBufferAccessible(HostAccess access);

        public abstract boolean isIterableAccessible(HostAccess access);

        public abstract boolean isIteratorAccessible(HostAccess access);

        public abstract boolean isMapAccessible(HostAccess access);

        public abstract Object getHostAccessImpl(HostAccess conf);

        public abstract void setHostAccessImpl(HostAccess conf, Object impl);

        public abstract UnmodifiableEconomicSet<String> getEvalAccess(PolyglotAccess access, String language);

        public abstract UnmodifiableEconomicSet<String> getBindingsAccess(PolyglotAccess access);

        public abstract String validatePolyglotAccess(PolyglotAccess access, UnmodifiableEconomicSet<String> language);

    }

    // shared SPI

    APIAccess api;
    ManagementAccess management;
    IOAccess io;

    AbstractPolyglotImpl next;

    public final void setMonitoring(ManagementAccess monitoring) {
        this.management = monitoring;
    }

    public final void setConstructors(APIAccess constructors) {
        this.api = constructors;
        initialize();
    }

    public final void setNext(AbstractPolyglotImpl next) {
        this.next = next;
    }

    public final AbstractPolyglotImpl getNext() {
        return next;
    }

    public final void setIO(IOAccess ioAccess) {
        Objects.requireNonNull(ioAccess, "IOAccess must be non null.");
        this.io = ioAccess;
    }

    public APIAccess getAPIAccess() {
        return api;
    }

    public ManagementAccess getManagement() {
        return management;
    }

    public final IOAccess getIO() {
        if (io == null) {
            try {
                Class.forName("org.graalvm.polyglot.io.IOHelper", true, getClass().getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        return io;
    }

    protected void initialize() {
    }

    public abstract Engine buildEngine(OutputStream out, OutputStream err, InputStream in, Map<String, String> options, boolean useSystemProperties, boolean allowExperimentalOptions,
                    boolean boundEngine,
                    MessageTransport messageInterceptor, Object logHandlerOrStream, EngineHostAccess conf);

    public abstract int getPriority();

    public abstract void preInitializeEngine();

    public abstract void resetPreInitializedEngine();

    public abstract AbstractSourceDispatch getSourceDispatch();

    public abstract AbstractSourceSectionDispatch getSourceSectionDispatch();

    public abstract AbstractManagementDispatch getManagementDispatch();

    /**
     * Returns the default host dispatch of this polyglot abstraction.
     */
    public abstract EngineHostAccess createHostAccess();

    public abstract static class AbstractManagementDispatch {

        AbstractPolyglotImpl polyglotImpl;

        protected AbstractManagementDispatch(AbstractPolyglotImpl polyglotImpl) {
            Objects.requireNonNull(polyglotImpl);
            this.polyglotImpl = polyglotImpl;
        }

        public final AbstractPolyglotImpl getPolyglotImpl() {
            return polyglotImpl;
        }

        public abstract List<Value> getExecutionEventInputValues(Object impl);

        public abstract SourceSection getExecutionEventLocation(Object impl);

        public abstract String getExecutionEventRootName(Object impl);

        public abstract Value getExecutionEventReturnValue(Object impl);

        public abstract boolean isExecutionEventExpression(Object impl);

        public abstract boolean isExecutionEventStatement(Object impl);

        public abstract boolean isExecutionEventRoot(Object impl);

        public abstract void closeExecutionListener(Object impl);

        public abstract Object attachExecutionListener(Object engine, Consumer<ExecutionEvent> onEnter,
                        Consumer<ExecutionEvent> onReturn,
                        boolean expressions,
                        boolean statements,
                        boolean roots,
                        Predicate<Source> sourceFilter, Predicate<String> rootFilter, boolean collectInputValues, boolean collectReturnValues, boolean collectExceptions);

        public abstract PolyglotException getExecutionEventException(Object impl);

    }

    public abstract static class AbstractSourceDispatch {

        protected final AbstractPolyglotImpl engineImpl;

        protected AbstractSourceDispatch(AbstractPolyglotImpl engineImpl) {
            Objects.requireNonNull(engineImpl);
            this.engineImpl = engineImpl;
        }

        public abstract Source build(String language, Object origin, URI uri, String name, String mimeType, Object content, boolean interactive, boolean internal, boolean cached, Charset encoding)
                        throws IOException;

        public abstract String getName(Object impl);

        public abstract String getPath(Object impl);

        public abstract boolean isInteractive(Object impl);

        public abstract URL getURL(Object impl);

        public abstract URI getURI(Object impl);

        public abstract Reader getReader(Object impl);

        public abstract InputStream getInputStream(Object impl);

        public abstract int getLength(Object impl);

        public abstract CharSequence getCharacters(Object impl);

        public abstract CharSequence getCharacters(Object impl, int lineNumber);

        public abstract int getLineCount(Object impl);

        public abstract int getLineNumber(Object impl, int offset);

        public abstract int getColumnNumber(Object impl, int offset);

        public abstract int getLineStartOffset(Object impl, int lineNumber);

        public abstract int getLineLength(Object impl, int lineNumber);

        public abstract String toString(Object impl);

        public abstract int hashCode(Object impl);

        public abstract boolean equals(Object impl, Object otherImpl);

        public abstract boolean isInternal(Object impl);

        public abstract String findLanguage(File file) throws IOException;

        public abstract String findLanguage(URL url) throws IOException;

        public abstract String findLanguage(String mimeType);

        public abstract String findMimeType(File file) throws IOException;

        public abstract String findMimeType(URL url) throws IOException;

        public abstract ByteSequence getBytes(Object impl);

        public abstract boolean hasCharacters(Object impl);

        public abstract boolean hasBytes(Object impl);

        public abstract String getMimeType(Object impl);

        public abstract String getLanguage(Object impl);

    }

    public abstract static class AbstractSourceSectionDispatch {

        protected AbstractSourceSectionDispatch(AbstractPolyglotImpl polyglotImpl) {
            Objects.requireNonNull(polyglotImpl);
        }

        public abstract boolean isAvailable(Object impl);

        public abstract boolean hasLines(Object impl);

        public abstract boolean hasColumns(Object impl);

        public abstract boolean hasCharIndex(Object impl);

        public abstract int getStartLine(Object impl);

        public abstract int getStartColumn(Object impl);

        public abstract int getEndLine(Object impl);

        public abstract int getEndColumn(Object impl);

        public abstract int getCharIndex(Object impl);

        public abstract int getCharLength(Object impl);

        public abstract int getCharEndIndex(Object impl);

        public abstract CharSequence getCode(Object impl);

        public abstract String toString(Object impl);

        public abstract int hashCode(Object impl);

        public abstract boolean equals(Object impl, Object obj);

    }

    public abstract static class AbstractContextDispatch {

        protected AbstractContextDispatch(AbstractPolyglotImpl impl) {
            Objects.requireNonNull(impl);
        }

        public abstract boolean initializeLanguage(Object receiver, String languageId);

        public abstract Value eval(Object receiver, String language, Object sourceImpl);

        public abstract Value parse(Object receiver, String language, Object sourceImpl);

        public abstract void close(Object receiver, boolean interuptExecution);

        public abstract boolean interrupt(Object receiver, Duration timeout);

        public abstract Value asValue(Object receiver, Object hostValue);

        public abstract void explicitEnter(Object receiver);

        public abstract void explicitLeave(Object receiver);

        public abstract Value getBindings(Object receiver, String language);

        public abstract Value getPolyglotBindings(Object receiver);

        public abstract void resetLimits(Object receiver);

        public abstract void safepoint(Object receiver);

        public abstract void setAPI(Object receiver, Context key);

    }

    public abstract static class AbstractEngineDispatch {

        protected AbstractEngineDispatch(AbstractPolyglotImpl impl) {
            Objects.requireNonNull(impl);
        }

        public abstract void setAPI(Object receiver, Engine key);

        public abstract Language requirePublicLanguage(Object receiver, String id);

        public abstract Instrument requirePublicInstrument(Object receiver, String id);

        // Runtime
        public abstract void close(Object receiver, Object apiObject, boolean cancelIfExecuting);

        public abstract Map<String, Instrument> getInstruments(Object receiver);

        public abstract Map<String, Language> getLanguages(Object receiver);

        public abstract OptionDescriptors getOptions(Object receiver);

        public abstract Context createContext(Object receiver, OutputStream out, OutputStream err, InputStream in, boolean allowHostAccess,
                        HostAccess hostAccess,
                        PolyglotAccess polyglotAccess,
                        boolean allowNativeAccess, boolean allowCreateThread, boolean allowHostIO, boolean allowHostClassLoading, boolean allowExperimentalOptions, Predicate<String> classFilter,
                        Map<String, String> options,
                        Map<String, String[]> arguments, String[] onlyLanguages, FileSystem fileSystem, Object logHandlerOrStream, boolean allowCreateProcess, ProcessHandler processHandler,
                        EnvironmentAccess environmentAccess, Map<String, String> environment, ZoneId zone, Object limitsImpl, String currentWorkingDirectory, ClassLoader hostClassLoader);

        public abstract String getImplementationName(Object receiver);

        public abstract Set<Source> getCachedSources(Object receiver);

        public abstract String getVersion(Object receiver);

    }

    public abstract static class AbstractExceptionDispatch {

        protected AbstractExceptionDispatch(AbstractPolyglotImpl engineImpl) {
            Objects.requireNonNull(engineImpl);
        }

        public abstract boolean isInternalError(Object receiver);

        public abstract boolean isCancelled(Object receiver);

        public abstract boolean isExit(Object receiver);

        public abstract int getExitStatus(Object receiver);

        public abstract Iterable<StackFrame> getPolyglotStackTrace(Object receiver);

        public abstract boolean isSyntaxError(Object receiver);

        public abstract Value getGuestObject(Object receiver);

        public abstract boolean isIncompleteSource(Object receiver);

        public abstract void onCreate(Object receiver, PolyglotException api);

        public abstract void printStackTrace(Object receiver, PrintStream s);

        public abstract void printStackTrace(Object receiver, PrintWriter s);

        public abstract StackTraceElement[] getStackTrace(Object receiver);

        public abstract String getMessage(Object receiver);

        public abstract boolean isHostException(Object receiver);

        public abstract Throwable asHostException(Object receiver);

        public abstract SourceSection getSourceLocation(Object receiver);

        public abstract boolean isResourceExhausted(Object receiver);

        public abstract boolean isInterrupted(Object receiver);

    }

    public abstract static class AbstractStackFrameImpl {

        protected AbstractStackFrameImpl(AbstractPolyglotImpl engineImpl) {
            Objects.requireNonNull(engineImpl);
        }

        public abstract StackTraceElement toHostFrame();

        public abstract SourceSection getSourceLocation();

        public abstract String getRootName();

        public abstract Language getLanguage();

        public abstract boolean isHostFrame();

        public abstract String toStringImpl(int languageColumn);

    }

    public abstract static class AbstractInstrumentDispatch {

        protected AbstractInstrumentDispatch(AbstractPolyglotImpl engineImpl) {
            Objects.requireNonNull(engineImpl);
        }

        public abstract String getId(Object receiver);

        public abstract String getName(Object receiver);

        public abstract OptionDescriptors getOptions(Object receiver);

        public abstract String getVersion(Object receiver);

        public abstract <T> T lookup(Object receiver, Class<T> type);

    }

    public abstract static class AbstractLanguageDispatch {

        protected AbstractLanguageDispatch(AbstractPolyglotImpl engineImpl) {
            Objects.requireNonNull(engineImpl);
        }

        public abstract String getName(Object receiver);

        public abstract String getImplementationName(Object receiver);

        public abstract boolean isInteractive(Object receiver);

        public abstract String getVersion(Object receiver);

        public abstract String getId(Object receiver);

        public abstract OptionDescriptors getOptions(Object receiver);

        public abstract Set<String> getMimeTypes(Object receiver);

        public abstract String getDefaultMimeType(Object receiver);
    }

    public abstract static class EngineHostAccess {

        protected EngineHostAccess(AbstractPolyglotImpl impl) {
            Objects.requireNonNull(impl);
        }

        public abstract Object createHostContext(HostAccess access, ClassLoader contextClassLoader);

        public abstract Object asHostValue(Object context, Object value);

        public abstract Object asHostDynamicClass(Object context, Class<?> value);

        public abstract Object asHostStaticClass(Object context, Class<?> value);

        public abstract boolean isHostValue(Object value);

        public abstract Object asHostValue(Object value);

        public abstract boolean isHostException(Object value);

        public abstract Throwable asHostException(Object value);

    }

    public abstract static class AbstractValueDispatch {

        protected AbstractValueDispatch(AbstractPolyglotImpl impl) {
            Objects.requireNonNull(impl);
        }

        public boolean hasArrayElements(Object receiver) {
            return false;
        }

        public abstract Value getArrayElement(Object receiver, long index);

        public abstract void setArrayElement(Object receiver, long index, Object value);

        public abstract boolean removeArrayElement(Object receiver, long index);

        public abstract long getArraySize(Object receiver);

        // region Buffer Methods

        public boolean hasBufferElements(Object receiver) {
            return false;
        }

        public abstract boolean isBufferWritable(Object receiver);

        public abstract long getBufferSize(Object receiver);

        public abstract byte readBufferByte(Object receiver, long byteOffset);

        public abstract void writeBufferByte(Object receiver, long byteOffset, byte value);

        public abstract short readBufferShort(Object receiver, ByteOrder order, long byteOffset);

        public abstract void writeBufferShort(Object receiver, ByteOrder order, long byteOffset, short value);

        public abstract int readBufferInt(Object receiver, ByteOrder order, long byteOffset);

        public abstract void writeBufferInt(Object receiver, ByteOrder order, long byteOffset, int value);

        public abstract long readBufferLong(Object receiver, ByteOrder order, long byteOffset);

        public abstract void writeBufferLong(Object receiver, ByteOrder order, long byteOffset, long value);

        public abstract float readBufferFloat(Object receiver, ByteOrder order, long byteOffset);

        public abstract void writeBufferFloat(Object receiver, ByteOrder order, long byteOffset, float value);

        public abstract double readBufferDouble(Object receiver, ByteOrder order, long byteOffset);

        public abstract void writeBufferDouble(Object receiver, ByteOrder order, long byteOffset, double value);

        // endregion

        public boolean hasMembers(Object receiver) {
            return false;
        }

        public abstract Value getMember(Object receiver, String key);

        public boolean hasMember(Object receiver, String key) {
            return false;
        }

        public Context getContext() {
            return null;
        }

        public Set<String> getMemberKeys(Object receiver) {
            return Collections.emptySet();
        }

        public abstract void putMember(Object receiver, String key, Object member);

        public abstract boolean removeMember(Object receiver, String key);

        public boolean canExecute(Object receiver) {
            return false;
        }

        public abstract Value execute(Object receiver, Object[] arguments);

        public abstract Value execute(Object receiver);

        public boolean canInstantiate(Object receiver) {
            return false;
        }

        public abstract Value newInstance(Object receiver, Object[] arguments);

        public abstract void executeVoid(Object receiver, Object[] arguments);

        public abstract void executeVoid(Object receiver);

        public boolean canInvoke(String identifier, Object receiver) {
            return false;
        }

        public abstract Value invoke(Object receiver, String identifier, Object[] arguments);

        public abstract Value invoke(Object receiver, String identifier);

        public boolean isString(Object receiver) {
            return false;
        }

        public abstract String asString(Object receiver);

        public boolean isBoolean(Object receiver) {
            return false;
        }

        public abstract boolean asBoolean(Object receiver);

        public boolean fitsInInt(Object receiver) {
            return false;
        }

        public abstract int asInt(Object receiver);

        public boolean fitsInLong(Object receiver) {
            return false;
        }

        public abstract long asLong(Object receiver);

        public boolean fitsInDouble(Object receiver) {
            return false;
        }

        public abstract double asDouble(Object receiver);

        public boolean fitsInFloat(Object receiver) {
            return false;
        }

        public abstract float asFloat(Object receiver);

        public boolean isNull(Object receiver) {
            return false;
        }

        public boolean isNativePointer(Object receiver) {
            return false;
        }

        public boolean fitsInByte(Object receiver) {
            return false;
        }

        public abstract byte asByte(Object receiver);

        public boolean fitsInShort(Object receiver) {
            return false;
        }

        public abstract short asShort(Object receiver);

        public abstract long asNativePointer(Object receiver);

        public boolean isHostObject(Object receiver) {
            return false;
        }

        public boolean isProxyObject(Object receiver) {
            return false;
        }

        public abstract Object asHostObject(Object receiver);

        public abstract Object asProxyObject(Object receiver);

        public abstract String toString(Object receiver);

        public abstract Value getMetaObject(Object receiver);

        public boolean isNumber(Object receiver) {
            return false;
        }

        public abstract <T> T as(Object receiver, Class<T> targetType);

        public abstract <T> T as(Object receiver, TypeLiteral<T> targetType);

        public abstract SourceSection getSourceLocation(Object receiver);

        public boolean isDate(Object receiver) {
            return false;
        }

        public abstract LocalDate asDate(Object receiver);

        public boolean isTime(Object receiver) {
            return false;
        }

        public abstract LocalTime asTime(Object receiver);

        public abstract Instant asInstant(Object receiver);

        public boolean isTimeZone(Object receiver) {
            return false;
        }

        public abstract ZoneId asTimeZone(Object receiver);

        public boolean isDuration(Object receiver) {
            return false;
        }

        public abstract Duration asDuration(Object receiver);

        public boolean isException(Object receiver) {
            return false;
        }

        public abstract RuntimeException throwException(Object receiver);

        public boolean isMetaObject(Object receiver) {
            return false;
        }

        public abstract String getMetaQualifiedName(Object receiver);

        public abstract String getMetaSimpleName(Object receiver);

        public abstract boolean isMetaInstance(Object receiver, Object instance);

        public abstract boolean equalsImpl(Object receiver, Object obj);

        public abstract int hashCodeImpl(Object receiver);

        public boolean hasIterator(Object receiver) {
            return false;
        }

        public abstract Value getIterator(Object receiver);

        public boolean isIterator(Object receiver) {
            return false;
        }

        public abstract boolean hasIteratorNextElement(Object receiver);

        public abstract Value getIteratorNextElement(Object receiver);

        public boolean hasHashEntries(Object receiver) {
            return false;
        }

        public abstract long getHashSize(Object receiver);

        public boolean hasHashEntry(Object receiver, Object key) {
            return false;
        }

        public abstract Value getHashValue(Object receiver, Object key);

        public abstract Value getHashValueOrDefault(Object receiver, Object key, Object defaultValue);

        public abstract void putHashEntry(Object receiver, Object key, Object value);

        public abstract boolean removeHashEntry(Object receiver, Object key);

        public abstract Value getHashEntriesIterator(Object receiver);

        public abstract Value getHashKeysIterator(Object receiver);

        public abstract Value getHashValuesIterator(Object receiver);
    }

    public abstract Class<?> loadLanguageClass(String className);

    public abstract Context getCurrentContext();

    public abstract Collection<? extends Object> findActiveEngines();

    public abstract Value asValue(Object o);

    public abstract <S, T> Object newTargetTypeMapping(Class<S> sourceType, Class<T> targetType, Predicate<S> acceptsValue, Function<S, T> convertValue, TargetMappingPrecedence precedence);

    public abstract Object buildLimits(long statementLimit, Predicate<Source> statementLimitSourceFilter, Consumer<ResourceLimitEvent> onLimit);

    public abstract FileSystem newDefaultFileSystem();

}
