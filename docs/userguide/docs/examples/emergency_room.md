# Emergency Room

Everyone is enjoying the summer, Covid19 restrictions have been lifted, we all get back to regular exercise and outdoor activities. But once in a while, the inevitable happens: An ill-considered step, a brief second of inattention, and injuries all of all types will happen, that require immediate treatment. Luckily our city hosts a modern hospital with an efficient emergency room where the wounded are being taken care of.

The simulation of hospital environment is a very popular. See [Günal 2010](https://www.tandfonline.com/doi/abs/10.1057/jos.2009.25?journalCode=tjsm20) for an excellent review.

To save more lives, the mayor has asked us to review and potentially improve process efficiency in the ER. To do so, we need to realize the following steps

1. Understand the current process and model is as simulation
2. Formulate key objectives to be optimized
2. Assess process statistics and metrics, to unravel potential improvements to help more patients.
3. Explore more optimized decision policies to increase

So let's dive right into it without further ado.

## Process Model


Patients are classified two-fold
1. By **Severity**. The ER is using the well known [Emergency Severity Index](https://en.wikipedia.org/wiki/Emergency_Severity_Index) to triage patients based on the acuity of patients' health care problems, and the number of resources their care is anticipated to require.
2. **Type of injury** which are defined [here](https://medlineplus.gov/woundsandinjuries.html)

Resources

* Surgery **rooms** that must be
  equipped by considering the type (i.e., the family) of surgery to
  be performed. It will take time to prepare a room for a certain type of injury. These setup times are listed in an excel sheet.
* **Doctors** that are qualified for a subset of all possible injuries

Process dynamics

* **PD-A** Depending on the severity, patients might die if not being treated. Also, if not being treated their severity will increase rather quickly
* **PD-B** The more busy the waiting room is, the less efficient surgeries tend to be. This is because of stress (over-allocation of supporting personal and material). It is phenomenon that is often observed complex queuing processes such as manufacturing or customer services.
* **PD-C** Depending on the severity, patients will die during surgery
* **PD-D** The surgery time correlates with the severity of the injury
* **PD-E** During nights fewer new patients arrive compared to the day

Clearly, more resources are required in the ER and many supported processes are required to run it. However, we leave these out here, as they are not considered to have a major impact on the overall process efficiency. Choosing a correct level of abstraction with a focus on key actors and resources, is the first _key to success_ when optimizing a complex process.


## Key Objectives & Observations

There are two competing processes. First, patient status will detoriate if not treated in time. Eventually, patients will pass away. The treatment process, counters and stops this first process. It must be maximized to minimize loss of life, minimize treatment efforts, while also maximizing treatment efficiency.

The head nurse, who is governing the process based on her long-term experience, is scheduling patients based on the following principle
 > Most urgent injuries first

[comment]: <> (   https://www.merriam-webster.com/dictionary/first%20come%2C%20first%20served)

Clearly if possible it would be great to also
* Minimize waiting times
* Reduce number of surgery room setups

## Analysis

Because of the great variety rooms, we observe a lot of setup steps to prepare surgery rooms. Often even if patients with the same type of injury all already waiting.

## Process Optimization

The idea for model above was orginally formulated by [Kramer et al. in 2019](todo reference) :
> Other relevant applications arise in the context of health-care, where, for example, patients have to be assigned to surgery rooms that must be  equipped by considering the type (i.e., the family) of surgery to be performed. In such cases, the weight usually models a level of urgency for the patient.


## Implementation

The tick-unit of the simulation is hours.


```kotlin
@file:Repository("*mavenLocal")
//@file:DependsOn("com.github.holgerbrandl:kalasim:0.6.97-SNAPSHOT")

//todo use artifact for release
//@file:DependsOn("com.github.holgerbrandl:kalasim:0.6.92")
//@file:DependsOn("com.github.holgerbrandl:kravis:0.8.1")
//@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
```


```kotlin
import org.kalasim.examples.er.*
import org.kalasim.*
import org.kalasim.monitors.MetricTimeline
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random
import org.kalasim.plot.kravis.display
import org.koin.core.qualifier.named


import org.kalasim.examples.er.*

kravis.SessionPrefs.OUTPUT_DEVICE = kravis.device.JupyterDevice() // bug in library,


```

## Simulation

Let's start by creating the model.


```kotlin
val er = EmergencyRoom(
    numPhysicians = 8,
    patientArrival = exponential(0.25.hours),
)

// enable log to capture events for later analysis
er.enableEventLog()

```


    java.lang.NoSuchMethodError: 'kotlin.time.Instant kotlinx.datetime.TimeZoneKt.atStartOfDayIn$default(kotlinx.datetime.LocalDate, kotlinx.datetime.TimeZone, kotlinx.datetime.OverloadMarker, int, java.lang.Object)'

    	at org.kalasim.TickTransformKt.somewhen(TickTransform.kt:128)

    	at org.kalasim.examples.er.EmergencyRoom.<init>(EmergencyRoom.kt:293)

    	at Line_52_jupyter.<init>(Line_52.jupyter.kts:1) at Cell In[47], line 1

    	at java.base/jdk.internal.reflect.DirectConstructorHandleAccessor.newInstance(DirectConstructorHandleAccessor.java:62)

    	at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:499)

    	at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:483)

    	at kotlin.script.experimental.jvm.BasicJvmScriptEvaluator.evalWithConfigAndOtherScriptsResults(BasicJvmScriptEvaluator.kt:122)

    	at kotlin.script.experimental.jvm.BasicJvmScriptEvaluator.invoke$suspendImpl(BasicJvmScriptEvaluator.kt:48)

    	at kotlin.script.experimental.jvm.BasicJvmScriptEvaluator.invoke(BasicJvmScriptEvaluator.kt)

    	at kotlin.script.experimental.jvm.BasicJvmReplEvaluator.eval(BasicJvmReplEvaluator.kt:49)

    	at org.jetbrains.kotlinx.jupyter.repl.impl.InternalEvaluatorImpl$eval$resultWithDiagnostics$1.invokeSuspend(InternalEvaluatorImpl.kt:137)

    	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:34)

    	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:100)

    	at kotlinx.coroutines.EventLoopImplBase.processNextEvent(EventLoop.common.kt:263)

    	at kotlinx.coroutines.BlockingCoroutine.joinBlocking(Builders.kt:94)

    	at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking(Builders.kt:70)

    	at kotlinx.coroutines.BuildersKt.runBlocking(Unknown Source)

    	at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking$default(Builders.kt:48)

    	at kotlinx.coroutines.BuildersKt.runBlocking$default(Unknown Source)

    	at org.jetbrains.kotlinx.jupyter.repl.impl.InternalEvaluatorImpl.eval(InternalEvaluatorImpl.kt:137)

    	at org.jetbrains.kotlinx.jupyter.repl.impl.CellExecutorImpl.execute_W38Nk0s$lambda$0$1(CellExecutorImpl.kt:95)

    	at org.jetbrains.kotlinx.jupyter.repl.impl.ReplForJupyterImpl.withHost(ReplForJupyterImpl.kt:730)

    	at org.jetbrains.kotlinx.jupyter.repl.impl.CellExecutorImpl.execute-W38Nk0s(CellExecutorImpl.kt:93)

    	at org.jetbrains.kotlinx.jupyter.repl.execution.CellExecutor.execute-W38Nk0s$default(CellExecutor.kt:14)

    	at org.jetbrains.kotlinx.jupyter.repl.impl.ReplForJupyterImpl.evaluateUserCode-wNURfNM(ReplForJupyterImpl.kt:591)

    	at org.jetbrains.kotlinx.jupyter.repl.impl.ReplForJupyterImpl.evalExImpl(ReplForJupyterImpl.kt:472)

    	at org.jetbrains.kotlinx.jupyter.repl.impl.ReplForJupyterImpl.evalEx$lambda$0(ReplForJupyterImpl.kt:466)

    	at org.jetbrains.kotlinx.jupyter.repl.impl.ReplForJupyterImpl.withEvalContext(ReplForJupyterImpl.kt:448)

    	at org.jetbrains.kotlinx.jupyter.repl.impl.ReplForJupyterImpl.evalEx(ReplForJupyterImpl.kt:465)

    	at org.jetbrains.kotlinx.jupyter.messaging.IdeCompatibleMessageRequestProcessor.processExecuteRequest$lambda$0$0$0(IdeCompatibleMessageRequestProcessor.kt:161)

    	at org.jetbrains.kotlinx.jupyter.streams.BlockingSubstitutionEngine.withDataSubstitution(SubstitutionEngine.kt:124)

    	at org.jetbrains.kotlinx.jupyter.streams.StreamSubstitutionManager.withSubstitutedStreams(StreamSubstitutionManager.kt:118)

    	at org.jetbrains.kotlinx.jupyter.messaging.IdeCompatibleMessageRequestProcessor.withForkedIn(IdeCompatibleMessageRequestProcessor.kt:351)

    	at org.jetbrains.kotlinx.jupyter.messaging.IdeCompatibleMessageRequestProcessor.evalWithIO$lambda$0$0(IdeCompatibleMessageRequestProcessor.kt:364)

    	at org.jetbrains.kotlinx.jupyter.streams.BlockingSubstitutionEngine.withDataSubstitution(SubstitutionEngine.kt:124)

    	at org.jetbrains.kotlinx.jupyter.streams.StreamSubstitutionManager.withSubstitutedStreams(StreamSubstitutionManager.kt:118)

    	at org.jetbrains.kotlinx.jupyter.messaging.IdeCompatibleMessageRequestProcessor.withForkedErr(IdeCompatibleMessageRequestProcessor.kt:341)

    	at org.jetbrains.kotlinx.jupyter.messaging.IdeCompatibleMessageRequestProcessor.evalWithIO$lambda$0(IdeCompatibleMessageRequestProcessor.kt:363)

    	at org.jetbrains.kotlinx.jupyter.streams.BlockingSubstitutionEngine.withDataSubstitution(SubstitutionEngine.kt:124)

    	at org.jetbrains.kotlinx.jupyter.streams.StreamSubstitutionManager.withSubstitutedStreams(StreamSubstitutionManager.kt:118)

    	at org.jetbrains.kotlinx.jupyter.messaging.IdeCompatibleMessageRequestProcessor.withForkedOut(IdeCompatibleMessageRequestProcessor.kt:334)

    	at org.jetbrains.kotlinx.jupyter.messaging.IdeCompatibleMessageRequestProcessor.evalWithIO(IdeCompatibleMessageRequestProcessor.kt:362)

    	at org.jetbrains.kotlinx.jupyter.messaging.IdeCompatibleMessageRequestProcessor.processExecuteRequest$lambda$0$0(IdeCompatibleMessageRequestProcessor.kt:160)

    	at org.jetbrains.kotlinx.jupyter.execution.JupyterExecutorImpl$Task.execute(JupyterExecutorImpl.kt:41)

    	at org.jetbrains.kotlinx.jupyter.execution.JupyterExecutorImpl.executorThread$lambda$0(JupyterExecutorImpl.kt:81)

    	at kotlin.concurrent.ThreadsKt$thread$thread$1.run(Thread.kt:30)

    

    java.lang.NoSuchMethodError: 'kotlin.time.Instant kotlinx.datetime.TimeZoneKt.atStartOfDayIn$default(kotlinx.datetime.LocalDate, kotlinx.datetime.TimeZone, kotlinx.datetime.OverloadMarker, int, java.lang.Object)'

    at Cell In[47], line 1


Now run it for some days, and print its state afterwards


```kotlin
er.run(7.days)

// print the object to render short status summary
er
```


```kotlin
er.waitingLine.sizeTimeline.display("Waiting Time")
```


```kotlin
er.treatedMonitor.display("Treated Patients")
```

Thats visualization is technically correct, but most likely we rather want to see treated patients per day



```kotlin
import org.kalasim.plot.kravis.displayStateCounts

er.patients.map{it.severity}.displayStateCounts()
```


```kotlin
 er.deceasedMonitor.display("Deceased Patients")
```



## Analysis

To analyze the model, we first use different visualization functinos defined for collection os [states](https://www.kalasim.org/state/), [components](https://www.kalasim.org/component/) and [resources](https://www.kalasim.org/resource/).


```kotlin
import org.kalasim.plot.kravis.displayTimelines

er.doctors.displayTimelines()

```


```kotlin
er.doctors.displayTimelines(byRequester = true, colorBy = { it.requester.name})
```

When studying the data from above, we observe that  an ER with more staff than available surgery rooms (e.g., `numPhysicians = 8` but only a limited number of rooms as defined in the `EmergencyRoom` example), we observe that the bottleneck shifts from staff availability to room availability.



```kotlin
import org.kalasim.plot.kravis.displayStateCounts

er.patients.map { it.patientStatus }.displayStateCounts()

```

Daily statistics vary slightly, but the overall distribution remains stable. This is expected since the current model does not yet include time-varying factors such as weekend shifts or staff changes.



```kotlin
er.patients.take(20).map { it.patientStatus }.displayTimelines(to = er.startDate + 4.hours)

```

We observe the different arrivals and their stay durations. Most of the sampled patients in the observed time window are going into surgey without a previous waiting time.


```kotlin
import org.kalasim.plot.kravis.displayStayDistributions

er.patients.map{ it.patientStatus }.displayStayDistributions()

```

The distribution of stay durations is heavily skewed towards shorter stays, indicating that most patients are discharged quickly. This could be due to the efficient triage and treatment processes in place.


Finally, as a DES model is driven by events, we inspect [raw events](https://www.kalasim.org/events/) from the model (using the [kotlin-dataframe](https://kotlin.github.io/dataframe/quickstart.html) interation)


```kotlin
import org.kalasim.analysis.InteractionEvent

val interactions = er.eventsInstanceOf<InteractionEvent>()

interactions.first()
```




    {"receiver":"main","details":"running; Hold +168.00, scheduled for 168.00","time":"2022-02-23T06:00:00Z","state":"SCHEDULED","type":"RescheduledEvent"}




```kotlin
val intDF = interactions.toDataFrame()
```


```kotlin
intDF.take(10)
```




            <iframe onload="o_resize_iframe_out_13()" style="width:100%;" class="result_container" id="iframe_out_13" frameBorder="0" srcdoc="        &lt;html&gt;
        &lt;head&gt;
            &lt;style type=&quot;text&sol;css&quot;&gt;
                :root {
    --background: #fff;
    --background-odd: #f5f5f5;
    --background-hover: #d9edfd;
    --header-text-color: #474747;
    --text-color: #848484;
    --text-color-dark: #000;
    --text-color-medium: #737373;
    --text-color-pale: #b3b3b3;
    --inner-border-color: #aaa;
    --bold-border-color: #000;
    --link-color: #296eaa;
    --link-color-pale: #296eaa;
    --link-hover: #1a466c;
}

:root[theme=&quot;dark&quot;], :root [data-jp-theme-light=&quot;false&quot;], .dataframe_dark{
    --background: #303030;
    --background-odd: #3c3c3c;
    --background-hover: #464646;
    --header-text-color: #dddddd;
    --text-color: #b3b3b3;
    --text-color-dark: #dddddd;
    --text-color-medium: #b2b2b2;
    --text-color-pale: #737373;
    --inner-border-color: #707070;
    --bold-border-color: #777777;
    --link-color: #008dc0;
    --link-color-pale: #97e1fb;
    --link-hover: #00688e;
}

p.dataframe_description {
    color: var(--text-color-dark);
}

table.dataframe {
    font-family: &quot;Helvetica Neue&quot;, Helvetica, Arial, sans-serif;
    font-size: 12px;
    background-color: var(--background);
    color: var(--text-color-dark);
    border: none;
    border-collapse: collapse;
}

table.dataframe th, td {
    padding: 6px;
    border: 1px solid transparent;
    text-align: left;
}

table.dataframe th {
    background-color: var(--background);
    color: var(--header-text-color);
}

table.dataframe td {
    vertical-align: top;
    white-space: nowrap;
}

table.dataframe th.bottomBorder {
    border-bottom-color: var(--bold-border-color);
}

table.dataframe tbody &gt; tr:nth-child(odd) {
    background: var(--background-odd);
}

table.dataframe tbody &gt; tr:nth-child(even) {
    background: var(--background);
}

table.dataframe tbody &gt; tr:hover {
    background: var(--background-hover);
}

table.dataframe a {
    cursor: pointer;
    color: var(--link-color);
    text-decoration: none;
}

table.dataframe tr:hover &gt; td a {
    color: var(--link-color-pale);
}

table.dataframe a:hover {
    color: var(--link-hover);
    text-decoration: underline;
}

table.dataframe img {
    max-width: fit-content;
}

table.dataframe th.complex {
    background-color: var(--background);
    border: 1px solid var(--background);
}

table.dataframe .leftBorder {
    border-left-color: var(--inner-border-color);
}

table.dataframe .rightBorder {
    border-right-color: var(--inner-border-color);
}

table.dataframe .rightAlign {
    text-align: right;
}

table.dataframe .expanderSvg {
    width: 8px;
    height: 8px;
    margin-right: 3px;
}

table.dataframe .expander {
    display: flex;
    align-items: center;
}

&sol;* formatting *&sol;

table.dataframe .null {
    color: var(--text-color-pale);
}

table.dataframe .structural {
    color: var(--text-color-medium);
    font-weight: bold;
}

table.dataframe .dataFrameCaption {
    font-weight: bold;
}

table.dataframe .numbers {
    color: var(--text-color-dark);
}

table.dataframe td:hover .formatted .structural, .null {
    color: var(--text-color-dark);
}

table.dataframe tr:hover .formatted .structural, .null {
    color: var(--text-color-dark);
}


:root {
    --scroll-bg: #f5f5f5;
    --scroll-fg: #b3b3b3;
}
:root[theme=&quot;dark&quot;], :root [data-jp-theme-light=&quot;false&quot;]{
    --scroll-bg: #3c3c3c;
    --scroll-fg: #97e1fb;
}
body {
    scrollbar-color: var(--scroll-fg) var(--scroll-bg);
}
body::-webkit-scrollbar {
    width: 10px; &sol;* Mostly for vertical scrollbars *&sol;
    height: 10px; &sol;* Mostly for horizontal scrollbars *&sol;
}
body::-webkit-scrollbar-thumb {
    background-color: var(--scroll-fg);
}
body::-webkit-scrollbar-track {
    background-color: var(--scroll-bg);
}
            &lt;&sol;style&gt;
        &lt;&sol;head&gt;
        &lt;body&gt;
            &lt;table class=&quot;dataframe&quot; id=&quot;df_16777249&quot;&gt;&lt;&sol;table&gt;

&lt;p class=&quot;dataframe_description&quot;&gt;DataFrame: rowsCount = 10, columnsCount = 7&lt;&sol;p&gt;

        &lt;&sol;body&gt;
        &lt;script&gt;
            (function () {
    window.DataFrame = window.DataFrame || new (function () {
        this.addTable = function (df) {
            let cols = df.cols;
            for (let i = 0; i &lt; cols.length; i++) {
                for (let c of cols[i].children) {
                    cols[c].parent = i;
                }
            }
            df.nrow = 0
            for (let i = 0; i &lt; df.cols.length; i++) {
                if (df.cols[i].values.length &gt; df.nrow) df.nrow = df.cols[i].values.length
            }
            if (df.id === df.rootId) {
                df.expandedFrames = new Set()
                df.childFrames = {}
                const table = this.getTableElement(df.id)
                table.df = df
                for (let i = 0; i &lt; df.cols.length; i++) {
                    let col = df.cols[i]
                    if (col.parent === undefined &amp;&amp; col.children.length &gt; 0) col.expanded = true
                }
            } else {
                const rootDf = this.getTableData(df.rootId)
                rootDf.childFrames[df.id] = df
            }
        }

        this.computeRenderData = function (df) {
            let result = []
            let pos = 0
            for (let col = 0; col &lt; df.cols.length; col++) {
                if (df.cols[col].parent === undefined)
                    pos += this.computeRenderDataRec(df.cols, col, pos, 0, result, false, false)
            }
            for (let i = 0; i &lt; result.length; i++) {
                let row = result[i]
                for (let j = 0; j &lt; row.length; j++) {
                    let cell = row[j]
                    if (j === 0)
                        cell.leftBd = false
                    if (j &lt; row.length - 1) {
                        let nextData = row[j + 1]
                        if (nextData.leftBd) cell.rightBd = true
                        else if (cell.rightBd) nextData.leftBd = true
                    } else cell.rightBd = false
                }
            }
            return result
        }

        this.computeRenderDataRec = function (cols, colId, pos, depth, result, leftBorder, rightBorder) {
            if (result.length === depth) {
                const array = [];
                if (pos &gt; 0) {
                    let j = 0
                    for (let i = 0; j &lt; pos; i++) {
                        let c = result[depth - 1][i]
                        j += c.span
                        let copy = Object.assign({empty: true}, c)
                        array.push(copy)
                    }
                }
                result.push(array)
            }
            const col = cols[colId];
            let size = 0;
            if (col.expanded) {
                let childPos = pos
                for (let i = 0; i &lt; col.children.length; i++) {
                    let child = col.children[i]
                    let childLeft = i === 0 &amp;&amp; (col.children.length &gt; 1 || leftBorder)
                    let childRight = i === col.children.length - 1 &amp;&amp; (col.children.length &gt; 1 || rightBorder)
                    let childSize = this.computeRenderDataRec(cols, child, childPos, depth + 1, result, childLeft, childRight)
                    childPos += childSize
                    size += childSize
                }
            } else {
                for (let i = depth + 1; i &lt; result.length; i++)
                    result[i].push({id: colId, span: 1, leftBd: leftBorder, rightBd: rightBorder, empty: true})
                size = 1
            }
            let left = leftBorder
            let right = rightBorder
            if (size &gt; 1) {
                left = true
                right = true
            }
            result[depth].push({id: colId, span: size, leftBd: left, rightBd: right})
            return size
        }

        this.getTableElement = function (id) {
            return document.getElementById(&quot;df_&quot; + id)
        }

        this.getTableData = function (id) {
            return this.getTableElement(id).df
        }

        this.createExpander = function (isExpanded) {
            const svgNs = &quot;http:&sol;&sol;www.w3.org&sol;2000&sol;svg&quot;
            let svg = document.createElementNS(svgNs, &quot;svg&quot;)
            svg.classList.add(&quot;expanderSvg&quot;)
            let path = document.createElementNS(svgNs, &quot;path&quot;)
            if (isExpanded) {
                svg.setAttribute(&quot;viewBox&quot;, &quot;0 -2 8 8&quot;)
                path.setAttribute(&quot;d&quot;, &quot;M1 0 l-1 1 4 4 4 -4 -1 -1 -3 3Z&quot;)
            } else {
                svg.setAttribute(&quot;viewBox&quot;, &quot;-2 0 8 8&quot;)
                path.setAttribute(&quot;d&quot;, &quot;M1 0 l-1 1 3 3 -3 3 1 1 4 -4Z&quot;)
            }
            path.setAttribute(&quot;fill&quot;, &quot;currentColor&quot;)
            svg.appendChild(path)
            return svg
        }

        this.renderTable = function (id) {

            let table = this.getTableElement(id)

            if (table === null) return

            table.innerHTML = &quot;&quot;

            let df = table.df
            let rootDf = df.rootId === df.id ? df : this.getTableData(df.rootId)

            &sol;&sol; header
            let header = document.createElement(&quot;thead&quot;)
            table.appendChild(header)

            let renderData = this.computeRenderData(df)
            for (let j = 0; j &lt; renderData.length; j++) {
                let rowData = renderData[j]
                let tr = document.createElement(&quot;tr&quot;);
                let isLastRow = j === renderData.length - 1
                header.appendChild(tr);
                for (let i = 0; i &lt; rowData.length; i++) {
                    let cell = rowData[i]
                    let th = document.createElement(&quot;th&quot;);
                    th.setAttribute(&quot;colspan&quot;, cell.span)
                    let colId = cell.id
                    let col = df.cols[colId];
                    if (!cell.empty) {
                        if (col.children.length === 0) {
                            th.innerHTML = col.name
                        } else {
                            let link = document.createElement(&quot;a&quot;)
                            link.className = &quot;expander&quot;
                            let that = this
                            link.onclick = function () {
                                col.expanded = !col.expanded
                                that.renderTable(id)
                            }
                            link.appendChild(this.createExpander(col.expanded))
                            link.innerHTML += col.name
                            th.appendChild(link)
                        }
                    }
                    let classes = (cell.leftBd ? &quot; leftBorder&quot; : &quot;&quot;) + (cell.rightBd ? &quot; rightBorder&quot; : &quot;&quot;)
                    if (col.rightAlign)
                        classes += &quot; rightAlign&quot;
                    if (isLastRow)
                        classes += &quot; bottomBorder&quot;
                    if (classes.length &gt; 0)
                        th.setAttribute(&quot;class&quot;, classes)
                    tr.appendChild(th)
                }
            }

            &sol;&sol; body
            let body = document.createElement(&quot;tbody&quot;)
            table.appendChild(body)

            let columns = renderData.pop()
            for (let row = 0; row &lt; df.nrow; row++) {
                let tr = document.createElement(&quot;tr&quot;);
                body.appendChild(tr)
                for (let i = 0; i &lt; columns.length; i++) {
                    let cell = columns[i]
                    let td = document.createElement(&quot;td&quot;);
                    let colId = cell.id
                    let col = df.cols[colId]
                    let classes = (cell.leftBd ? &quot; leftBorder&quot; : &quot;&quot;) + (cell.rightBd ? &quot; rightBorder&quot; : &quot;&quot;)
                    if (col.rightAlign)
                        classes += &quot; rightAlign&quot;
                    if (classes.length &gt; 0)
                        td.setAttribute(&quot;class&quot;, classes)
                    tr.appendChild(td)
                    let value = col.values[row]
                    if (value.frameId !== undefined) {
                        let frameId = value.frameId
                        let expanded = rootDf.expandedFrames.has(frameId)
                        let link = document.createElement(&quot;a&quot;)
                        link.className = &quot;expander&quot;
                        let that = this
                        link.onclick = function () {
                            if (rootDf.expandedFrames.has(frameId))
                                rootDf.expandedFrames.delete(frameId)
                            else rootDf.expandedFrames.add(frameId)
                            that.renderTable(id)
                        }
                        link.appendChild(this.createExpander(expanded))
                        link.innerHTML += value.value
                        if (expanded) {
                            td.appendChild(link)
                            td.appendChild(document.createElement(&quot;p&quot;))
                            const childTable = document.createElement(&quot;table&quot;)
                            childTable.className = &quot;dataframe&quot;
                            childTable.id = &quot;df_&quot; + frameId
                            let childDf = rootDf.childFrames[frameId]
                            childTable.df = childDf
                            td.appendChild(childTable)
                            this.renderTable(frameId)
                            if (childDf.nrow !== childDf.totalRows) {
                                const footer = document.createElement(&quot;p&quot;)
                                footer.innerText = `... showing only top ${childDf.nrow} of ${childDf.totalRows} rows`
                                td.appendChild(footer)
                            }
                        } else {
                            td.appendChild(link)
                        }
                    } else if (value.style !== undefined) {
                        td.innerHTML = value.value
                        td.setAttribute(&quot;style&quot;, value.style)
                    } else td.innerHTML = value
                    this.nodeScriptReplace(td)
                }
            }
        }

        this.nodeScriptReplace = function (node) {
            if (this.nodeScriptIs(node) === true) {
                node.parentNode.replaceChild(this.nodeScriptClone(node), node);
            } else {
                let i = -1, children = node.childNodes;
                while (++i &lt; children.length) {
                    this.nodeScriptReplace(children[i]);
                }
            }

            return node;
        }

        this.nodeScriptClone = function (node) {
            let script = document.createElement(&quot;script&quot;);
            script.text = node.innerHTML;

            let i = -1, attrs = node.attributes, attr;
            while (++i &lt; attrs.length) {
                script.setAttribute((attr = attrs[i]).name, attr.value);
            }
            return script;
        }

        this.nodeScriptIs = function (node) {
            return node.tagName === 'SCRIPT';
        }
    })()

    window.call_DataFrame = function (f) {
        return f();
    };

    let funQueue = window[&quot;kotlinQueues&quot;] &amp;&amp; window[&quot;kotlinQueues&quot;][&quot;DataFrame&quot;];
    if (funQueue) {
        funQueue.forEach(function (f) {
            f();
        });
        funQueue = [];
    }
})()

&sol;*&lt;!--*&sol;
call_DataFrame(function() { DataFrame.addTable({ cols: [{ name: &quot;&lt;span title=&bsol;&quot;time: kotlinx.datetime.Instant&bsol;&quot;&gt;time&lt;&sol;span&gt;&quot;, children: [], rightAlign: false, values: [&quot;2022-02-23T06:00:00Z&quot;,&quot;2022-02-23T06:00:00Z&quot;,&quot;2022-02-23T06:00:00Z&quot;,&quot;2022-02-23T06:00:00Z&quot;,&quot;2022-02-23T06:00:00Z&quot;,&quot;2022-02-23T06:00:00Z&quot;,&quot;2022-02-23T06:04:57.988113230Z&quot;,&quot;2022-02-23T06:04:57.988113230Z&quot;,&quot;2022-02-23T06:04:57.988113230Z&quot;,&quot;2022-02-23T06:16:54.513625724Z&quot;] }, 
{ name: &quot;&lt;span title=&bsol;&quot;current: org.kalasim.Component?&bsol;&quot;&gt;current&lt;&sol;span&gt;&quot;, children: [], rightAlign: false, values: [&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;null&bsol;&quot;&gt;null&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;room 0&quot;,&quot;room 1&quot;,&quot;room 2&quot;,&quot;room 3&quot;,&quot;ComponentGenerator.1&quot;,&quot;ComponentGenerator.1&quot;,&quot;ComponentGenerator.1&quot;,&quot;ComponentGenerator.1&quot;,&quot;ComponentGenerator.1&quot;] }, 
{ name: &quot;&lt;span title=&bsol;&quot;component: org.kalasim.Component&bsol;&quot;&gt;component&lt;&sol;span&gt;&quot;, children: [], rightAlign: false, values: [&quot;main&quot;,&quot;room 0&quot;,&quot;room 1&quot;,&quot;room 2&quot;,&quot;room 3&quot;,&quot;ComponentGenerator.1&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;Patient(fullName=Armando Kessler, patientId=0, type=Scratches, severity=Armando Kessler[Emergent], patientStatus=Armando Kessler[Waiting])&bsol;&quot;&gt;Patient(fullName=Armando Kessler, pat&lt;span class=&bsol;&quot;structural&bsol;&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;Patient(fullName=Armando Kessler, patientId=0, type=Scratches, severity=Armando Kessler[Emergent], patientStatus=Armando Kessler[Waiting])&bsol;&quot;&gt;Patient(fullName=Armando Kessler, pat&lt;span class=&bsol;&quot;structural&bsol;&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;ComponentGenerator.1&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;Patient(fullName=Laurie Keebler, patientId=1, type=Dislocations, severity=Laurie Keebler[Resuscitation], patientStatus=Laurie Keebler[Waiting])&bsol;&quot;&gt;Patient(fullName=Laurie Keebler, pati&lt;span class=&bsol;&quot;structural&bsol;&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;] }, 
{ name: &quot;&lt;span title=&bsol;&quot;actionFn: Function0&lt;String&gt;?&bsol;&quot;&gt;actionFn&lt;&sol;span&gt;&quot;, children: [], rightAlign: false, values: [&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;null&bsol;&quot;&gt;null&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;org.kalasim.analysis.EventsKt$$Lambda&sol;0x00000000938df648@119dcad2&bsol;&quot;&gt;org.kalasim.analysis.EventsKt$$Lambda&lt;span class=&bsol;&quot;structural&bsol;&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;org.kalasim.analysis.EventsKt$$Lambda&sol;0x00000000938df648@e128865&bsol;&quot;&gt;org.kalasim.analysis.EventsKt$$Lambda&lt;span class=&bsol;&quot;structural&bsol;&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;org.kalasim.analysis.EventsKt$$Lambda&sol;0x00000000938df648@2e206a70&bsol;&quot;&gt;org.kalasim.analysis.EventsKt$$Lambda&lt;span class=&bsol;&quot;structural&bsol;&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;org.kalasim.analysis.EventsKt$$Lambda&sol;0x00000000938df648@63df4754&bsol;&quot;&gt;org.kalasim.analysis.EventsKt$$Lambda&lt;span class=&bsol;&quot;structural&bsol;&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;null&bsol;&quot;&gt;null&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;null&bsol;&quot;&gt;null&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;org.kalasim.analysis.EventsKt$$Lambda&sol;0x00000000938df648@1cebdaa7&bsol;&quot;&gt;org.kalasim.analysis.EventsKt$$Lambda&lt;span class=&bsol;&quot;structural&bsol;&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;null&bsol;&quot;&gt;null&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;null&bsol;&quot;&gt;null&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;] }, 
{ name: &quot;&lt;span title=&bsol;&quot;action: String&bsol;&quot;&gt;action&lt;&sol;span&gt;&quot;, children: [], rightAlign: false, values: [&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;running; Hold +168.00, scheduled for 168.00&bsol;&quot;&gt;running; Hold +168.00, scheduled for &lt;span class=&bsol;&quot;structural&bsol;&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;canceled&quot;,&quot;canceled&quot;,&quot;canceled&quot;,&quot;canceled&quot;,&quot;Hold +.08, scheduled for .08&quot;,&quot;Activated, scheduled for .08&quot;,&quot;canceled&quot;,&quot;Hold +.20, scheduled for .28&quot;,&quot;Activated, scheduled for .28&quot;] }, 
{ name: &quot;&lt;span title=&bsol;&quot;eventType: String&bsol;&quot;&gt;eventType&lt;&sol;span&gt;&quot;, children: [], rightAlign: false, values: [&quot;RescheduledEvent&quot;,&quot;ComponentStateChangeEvent&quot;,&quot;ComponentStateChangeEvent&quot;,&quot;ComponentStateChangeEvent&quot;,&quot;ComponentStateChangeEvent&quot;,&quot;RescheduledEvent&quot;,&quot;RescheduledEvent&quot;,&quot;ComponentStateChangeEvent&quot;,&quot;RescheduledEvent&quot;,&quot;RescheduledEvent&quot;] }, 
{ name: &quot;&lt;span title=&bsol;&quot;tickTime: Long&bsol;&quot;&gt;tickTime&lt;&sol;span&gt;&quot;, children: [], rightAlign: true, values: [&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;numbers&bsol;&quot;&gt;1645596000&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;numbers&bsol;&quot;&gt;1645596000&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;numbers&bsol;&quot;&gt;1645596000&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;numbers&bsol;&quot;&gt;1645596000&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;numbers&bsol;&quot;&gt;1645596000&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;numbers&bsol;&quot;&gt;1645596000&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;numbers&bsol;&quot;&gt;1645596297&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;numbers&bsol;&quot;&gt;1645596297&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;numbers&bsol;&quot;&gt;1645596297&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;numbers&bsol;&quot;&gt;1645597014&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;] }, 
], id: 16777249, rootId: 16777249, totalRows: 10 } ) });
&sol;*--&gt;*&sol;

call_DataFrame(function() { DataFrame.renderTable(16777249) });


        &lt;&sol;script&gt;
        &lt;&sol;html&gt;"></iframe>
            <script>
                function o_resize_iframe_out_13() {
                    let elem = document.getElementById("iframe_out_13");
                    resize_iframe_out_13(elem);
                    setInterval(resize_iframe_out_13, 5000, elem);
                }
                function resize_iframe_out_13(el) {
                    let h = el.contentWindow.document.body.scrollHeight;
                    el.height = h === 0 ? 0 : h + 41;
                }
            </script>        <html>
        <head>
            <style type="text/css">
                :root {
    --background: #fff;
    --background-odd: #f5f5f5;
    --background-hover: #d9edfd;
    --header-text-color: #474747;
    --text-color: #848484;
    --text-color-dark: #000;
    --text-color-medium: #737373;
    --text-color-pale: #b3b3b3;
    --inner-border-color: #aaa;
    --bold-border-color: #000;
    --link-color: #296eaa;
    --link-color-pale: #296eaa;
    --link-hover: #1a466c;
}

:root[theme="dark"], :root [data-jp-theme-light="false"], .dataframe_dark{
    --background: #303030;
    --background-odd: #3c3c3c;
    --background-hover: #464646;
    --header-text-color: #dddddd;
    --text-color: #b3b3b3;
    --text-color-dark: #dddddd;
    --text-color-medium: #b2b2b2;
    --text-color-pale: #737373;
    --inner-border-color: #707070;
    --bold-border-color: #777777;
    --link-color: #008dc0;
    --link-color-pale: #97e1fb;
    --link-hover: #00688e;
}

p.dataframe_description {
    color: var(--text-color-dark);
}

table.dataframe {
    font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
    font-size: 12px;
    background-color: var(--background);
    color: var(--text-color-dark);
    border: none;
    border-collapse: collapse;
}

table.dataframe th, td {
    padding: 6px;
    border: 1px solid transparent;
    text-align: left;
}

table.dataframe th {
    background-color: var(--background);
    color: var(--header-text-color);
}

table.dataframe td {
    vertical-align: top;
    white-space: nowrap;
}

table.dataframe th.bottomBorder {
    border-bottom-color: var(--bold-border-color);
}

table.dataframe tbody > tr:nth-child(odd) {
    background: var(--background-odd);
}

table.dataframe tbody > tr:nth-child(even) {
    background: var(--background);
}

table.dataframe tbody > tr:hover {
    background: var(--background-hover);
}

table.dataframe a {
    cursor: pointer;
    color: var(--link-color);
    text-decoration: none;
}

table.dataframe tr:hover > td a {
    color: var(--link-color-pale);
}

table.dataframe a:hover {
    color: var(--link-hover);
    text-decoration: underline;
}

table.dataframe img {
    max-width: fit-content;
}

table.dataframe th.complex {
    background-color: var(--background);
    border: 1px solid var(--background);
}

table.dataframe .leftBorder {
    border-left-color: var(--inner-border-color);
}

table.dataframe .rightBorder {
    border-right-color: var(--inner-border-color);
}

table.dataframe .rightAlign {
    text-align: right;
}

table.dataframe .expanderSvg {
    width: 8px;
    height: 8px;
    margin-right: 3px;
}

table.dataframe .expander {
    display: flex;
    align-items: center;
}

/* formatting */

table.dataframe .null {
    color: var(--text-color-pale);
}

table.dataframe .structural {
    color: var(--text-color-medium);
    font-weight: bold;
}

table.dataframe .dataFrameCaption {
    font-weight: bold;
}

table.dataframe .numbers {
    color: var(--text-color-dark);
}

table.dataframe td:hover .formatted .structural, .null {
    color: var(--text-color-dark);
}

table.dataframe tr:hover .formatted .structural, .null {
    color: var(--text-color-dark);
}


            </style>
        </head>
        <body>
            <table class="dataframe" id="static_df_16777250"><thead><tr><th class="bottomBorder" style="text-align:left">time</th><th class="bottomBorder" style="text-align:left">current</th><th class="bottomBorder" style="text-align:left">component</th><th class="bottomBorder" style="text-align:left">actionFn</th><th class="bottomBorder" style="text-align:left">action</th><th class="bottomBorder" style="text-align:left">eventType</th><th class="bottomBorder" style="text-align:left">tickTime</th></tr></thead><tbody><tr><td  style="vertical-align:top">2022-02-23T06:00:00Z</td><td  style="vertical-align:top">null</td><td  style="vertical-align:top">main</td><td  style="vertical-align:top">null</td><td  style="vertical-align:top">running; Hold +168.00, scheduled for <span class="structural">...</span></td><td  style="vertical-align:top">RescheduledEvent</td><td  style="vertical-align:top">1645596000</td></tr><tr><td  style="vertical-align:top">2022-02-23T06:00:00Z</td><td  style="vertical-align:top">room 0</td><td  style="vertical-align:top">room 0</td><td  style="vertical-align:top">org.kalasim.analysis.EventsKt$$Lambda<span class="structural">...</span></td><td  style="vertical-align:top">canceled</td><td  style="vertical-align:top">ComponentStateChangeEvent</td><td  style="vertical-align:top">1645596000</td></tr><tr><td  style="vertical-align:top">2022-02-23T06:00:00Z</td><td  style="vertical-align:top">room 1</td><td  style="vertical-align:top">room 1</td><td  style="vertical-align:top">org.kalasim.analysis.EventsKt$$Lambda<span class="structural">...</span></td><td  style="vertical-align:top">canceled</td><td  style="vertical-align:top">ComponentStateChangeEvent</td><td  style="vertical-align:top">1645596000</td></tr><tr><td  style="vertical-align:top">2022-02-23T06:00:00Z</td><td  style="vertical-align:top">room 2</td><td  style="vertical-align:top">room 2</td><td  style="vertical-align:top">org.kalasim.analysis.EventsKt$$Lambda<span class="structural">...</span></td><td  style="vertical-align:top">canceled</td><td  style="vertical-align:top">ComponentStateChangeEvent</td><td  style="vertical-align:top">1645596000</td></tr><tr><td  style="vertical-align:top">2022-02-23T06:00:00Z</td><td  style="vertical-align:top">room 3</td><td  style="vertical-align:top">room 3</td><td  style="vertical-align:top">org.kalasim.analysis.EventsKt$$Lambda<span class="structural">...</span></td><td  style="vertical-align:top">canceled</td><td  style="vertical-align:top">ComponentStateChangeEvent</td><td  style="vertical-align:top">1645596000</td></tr><tr><td  style="vertical-align:top">2022-02-23T06:00:00Z</td><td  style="vertical-align:top">ComponentGenerator.1</td><td  style="vertical-align:top">ComponentGenerator.1</td><td  style="vertical-align:top">null</td><td  style="vertical-align:top">Hold +.08, scheduled for .08</td><td  style="vertical-align:top">RescheduledEvent</td><td  style="vertical-align:top">1645596000</td></tr><tr><td  style="vertical-align:top">2022-02-23T06:04:57.988113230Z</td><td  style="vertical-align:top">ComponentGenerator.1</td><td  style="vertical-align:top">Patient(fullName=Armando Kessler, pat<span class="structural">...</span></td><td  style="vertical-align:top">null</td><td  style="vertical-align:top">Activated, scheduled for .08</td><td  style="vertical-align:top">RescheduledEvent</td><td  style="vertical-align:top">1645596297</td></tr><tr><td  style="vertical-align:top">2022-02-23T06:04:57.988113230Z</td><td  style="vertical-align:top">ComponentGenerator.1</td><td  style="vertical-align:top">Patient(fullName=Armando Kessler, pat<span class="structural">...</span></td><td  style="vertical-align:top">org.kalasim.analysis.EventsKt$$Lambda<span class="structural">...</span></td><td  style="vertical-align:top">canceled</td><td  style="vertical-align:top">ComponentStateChangeEvent</td><td  style="vertical-align:top">1645596297</td></tr><tr><td  style="vertical-align:top">2022-02-23T06:04:57.988113230Z</td><td  style="vertical-align:top">ComponentGenerator.1</td><td  style="vertical-align:top">ComponentGenerator.1</td><td  style="vertical-align:top">null</td><td  style="vertical-align:top">Hold +.20, scheduled for .28</td><td  style="vertical-align:top">RescheduledEvent</td><td  style="vertical-align:top">1645596297</td></tr><tr><td  style="vertical-align:top">2022-02-23T06:16:54.513625724Z</td><td  style="vertical-align:top">ComponentGenerator.1</td><td  style="vertical-align:top">Patient(fullName=Laurie Keebler, pati<span class="structural">...</span></td><td  style="vertical-align:top">null</td><td  style="vertical-align:top">Activated, scheduled for .28</td><td  style="vertical-align:top">RescheduledEvent</td><td  style="vertical-align:top">1645597014</td></tr></tbody></table>
        </body>
        <script>
            document.getElementById("static_df_16777250").style.display = "none";
        </script>
        </html>




```kotlin
intDF.groupBy { expr { eventType } }
    .count()
```




            <iframe onload="o_resize_iframe_out_10()" style="width:100%;" class="result_container" id="iframe_out_10" frameBorder="0" srcdoc="        &lt;html&gt;
        &lt;head&gt;
            &lt;style type=&quot;text&sol;css&quot;&gt;
                :root {
    --background: #fff;
    --background-odd: #f5f5f5;
    --background-hover: #d9edfd;
    --header-text-color: #474747;
    --text-color: #848484;
    --text-color-dark: #000;
    --text-color-medium: #737373;
    --text-color-pale: #b3b3b3;
    --inner-border-color: #aaa;
    --bold-border-color: #000;
    --link-color: #296eaa;
    --link-color-pale: #296eaa;
    --link-hover: #1a466c;
}

:root[theme=&quot;dark&quot;], :root [data-jp-theme-light=&quot;false&quot;], .dataframe_dark{
    --background: #303030;
    --background-odd: #3c3c3c;
    --background-hover: #464646;
    --header-text-color: #dddddd;
    --text-color: #b3b3b3;
    --text-color-dark: #dddddd;
    --text-color-medium: #b2b2b2;
    --text-color-pale: #737373;
    --inner-border-color: #707070;
    --bold-border-color: #777777;
    --link-color: #008dc0;
    --link-color-pale: #97e1fb;
    --link-hover: #00688e;
}

p.dataframe_description {
    color: var(--text-color-dark);
}

table.dataframe {
    font-family: &quot;Helvetica Neue&quot;, Helvetica, Arial, sans-serif;
    font-size: 12px;
    background-color: var(--background);
    color: var(--text-color-dark);
    border: none;
    border-collapse: collapse;
}

table.dataframe th, td {
    padding: 6px;
    border: 1px solid transparent;
    text-align: left;
}

table.dataframe th {
    background-color: var(--background);
    color: var(--header-text-color);
}

table.dataframe td {
    vertical-align: top;
    white-space: nowrap;
}

table.dataframe th.bottomBorder {
    border-bottom-color: var(--bold-border-color);
}

table.dataframe tbody &gt; tr:nth-child(odd) {
    background: var(--background-odd);
}

table.dataframe tbody &gt; tr:nth-child(even) {
    background: var(--background);
}

table.dataframe tbody &gt; tr:hover {
    background: var(--background-hover);
}

table.dataframe a {
    cursor: pointer;
    color: var(--link-color);
    text-decoration: none;
}

table.dataframe tr:hover &gt; td a {
    color: var(--link-color-pale);
}

table.dataframe a:hover {
    color: var(--link-hover);
    text-decoration: underline;
}

table.dataframe img {
    max-width: fit-content;
}

table.dataframe th.complex {
    background-color: var(--background);
    border: 1px solid var(--background);
}

table.dataframe .leftBorder {
    border-left-color: var(--inner-border-color);
}

table.dataframe .rightBorder {
    border-right-color: var(--inner-border-color);
}

table.dataframe .rightAlign {
    text-align: right;
}

table.dataframe .expanderSvg {
    width: 8px;
    height: 8px;
    margin-right: 3px;
}

table.dataframe .expander {
    display: flex;
    align-items: center;
}

&sol;* formatting *&sol;

table.dataframe .null {
    color: var(--text-color-pale);
}

table.dataframe .structural {
    color: var(--text-color-medium);
    font-weight: bold;
}

table.dataframe .dataFrameCaption {
    font-weight: bold;
}

table.dataframe .numbers {
    color: var(--text-color-dark);
}

table.dataframe td:hover .formatted .structural, .null {
    color: var(--text-color-dark);
}

table.dataframe tr:hover .formatted .structural, .null {
    color: var(--text-color-dark);
}


:root {
    --scroll-bg: #f5f5f5;
    --scroll-fg: #b3b3b3;
}
:root[theme=&quot;dark&quot;], :root [data-jp-theme-light=&quot;false&quot;]{
    --scroll-bg: #3c3c3c;
    --scroll-fg: #97e1fb;
}
body {
    scrollbar-color: var(--scroll-fg) var(--scroll-bg);
}
body::-webkit-scrollbar {
    width: 10px; &sol;* Mostly for vertical scrollbars *&sol;
    height: 10px; &sol;* Mostly for horizontal scrollbars *&sol;
}
body::-webkit-scrollbar-thumb {
    background-color: var(--scroll-fg);
}
body::-webkit-scrollbar-track {
    background-color: var(--scroll-bg);
}
            &lt;&sol;style&gt;
        &lt;&sol;head&gt;
        &lt;body&gt;
            &lt;table class=&quot;dataframe&quot; id=&quot;df_16777243&quot;&gt;&lt;&sol;table&gt;

&lt;p class=&quot;dataframe_description&quot;&gt;DataFrame: rowsCount = 5, columnsCount = 2&lt;&sol;p&gt;

        &lt;&sol;body&gt;
        &lt;script&gt;
            (function () {
    window.DataFrame = window.DataFrame || new (function () {
        this.addTable = function (df) {
            let cols = df.cols;
            for (let i = 0; i &lt; cols.length; i++) {
                for (let c of cols[i].children) {
                    cols[c].parent = i;
                }
            }
            df.nrow = 0
            for (let i = 0; i &lt; df.cols.length; i++) {
                if (df.cols[i].values.length &gt; df.nrow) df.nrow = df.cols[i].values.length
            }
            if (df.id === df.rootId) {
                df.expandedFrames = new Set()
                df.childFrames = {}
                const table = this.getTableElement(df.id)
                table.df = df
                for (let i = 0; i &lt; df.cols.length; i++) {
                    let col = df.cols[i]
                    if (col.parent === undefined &amp;&amp; col.children.length &gt; 0) col.expanded = true
                }
            } else {
                const rootDf = this.getTableData(df.rootId)
                rootDf.childFrames[df.id] = df
            }
        }

        this.computeRenderData = function (df) {
            let result = []
            let pos = 0
            for (let col = 0; col &lt; df.cols.length; col++) {
                if (df.cols[col].parent === undefined)
                    pos += this.computeRenderDataRec(df.cols, col, pos, 0, result, false, false)
            }
            for (let i = 0; i &lt; result.length; i++) {
                let row = result[i]
                for (let j = 0; j &lt; row.length; j++) {
                    let cell = row[j]
                    if (j === 0)
                        cell.leftBd = false
                    if (j &lt; row.length - 1) {
                        let nextData = row[j + 1]
                        if (nextData.leftBd) cell.rightBd = true
                        else if (cell.rightBd) nextData.leftBd = true
                    } else cell.rightBd = false
                }
            }
            return result
        }

        this.computeRenderDataRec = function (cols, colId, pos, depth, result, leftBorder, rightBorder) {
            if (result.length === depth) {
                const array = [];
                if (pos &gt; 0) {
                    let j = 0
                    for (let i = 0; j &lt; pos; i++) {
                        let c = result[depth - 1][i]
                        j += c.span
                        let copy = Object.assign({empty: true}, c)
                        array.push(copy)
                    }
                }
                result.push(array)
            }
            const col = cols[colId];
            let size = 0;
            if (col.expanded) {
                let childPos = pos
                for (let i = 0; i &lt; col.children.length; i++) {
                    let child = col.children[i]
                    let childLeft = i === 0 &amp;&amp; (col.children.length &gt; 1 || leftBorder)
                    let childRight = i === col.children.length - 1 &amp;&amp; (col.children.length &gt; 1 || rightBorder)
                    let childSize = this.computeRenderDataRec(cols, child, childPos, depth + 1, result, childLeft, childRight)
                    childPos += childSize
                    size += childSize
                }
            } else {
                for (let i = depth + 1; i &lt; result.length; i++)
                    result[i].push({id: colId, span: 1, leftBd: leftBorder, rightBd: rightBorder, empty: true})
                size = 1
            }
            let left = leftBorder
            let right = rightBorder
            if (size &gt; 1) {
                left = true
                right = true
            }
            result[depth].push({id: colId, span: size, leftBd: left, rightBd: right})
            return size
        }

        this.getTableElement = function (id) {
            return document.getElementById(&quot;df_&quot; + id)
        }

        this.getTableData = function (id) {
            return this.getTableElement(id).df
        }

        this.createExpander = function (isExpanded) {
            const svgNs = &quot;http:&sol;&sol;www.w3.org&sol;2000&sol;svg&quot;
            let svg = document.createElementNS(svgNs, &quot;svg&quot;)
            svg.classList.add(&quot;expanderSvg&quot;)
            let path = document.createElementNS(svgNs, &quot;path&quot;)
            if (isExpanded) {
                svg.setAttribute(&quot;viewBox&quot;, &quot;0 -2 8 8&quot;)
                path.setAttribute(&quot;d&quot;, &quot;M1 0 l-1 1 4 4 4 -4 -1 -1 -3 3Z&quot;)
            } else {
                svg.setAttribute(&quot;viewBox&quot;, &quot;-2 0 8 8&quot;)
                path.setAttribute(&quot;d&quot;, &quot;M1 0 l-1 1 3 3 -3 3 1 1 4 -4Z&quot;)
            }
            path.setAttribute(&quot;fill&quot;, &quot;currentColor&quot;)
            svg.appendChild(path)
            return svg
        }

        this.renderTable = function (id) {

            let table = this.getTableElement(id)

            if (table === null) return

            table.innerHTML = &quot;&quot;

            let df = table.df
            let rootDf = df.rootId === df.id ? df : this.getTableData(df.rootId)

            &sol;&sol; header
            let header = document.createElement(&quot;thead&quot;)
            table.appendChild(header)

            let renderData = this.computeRenderData(df)
            for (let j = 0; j &lt; renderData.length; j++) {
                let rowData = renderData[j]
                let tr = document.createElement(&quot;tr&quot;);
                let isLastRow = j === renderData.length - 1
                header.appendChild(tr);
                for (let i = 0; i &lt; rowData.length; i++) {
                    let cell = rowData[i]
                    let th = document.createElement(&quot;th&quot;);
                    th.setAttribute(&quot;colspan&quot;, cell.span)
                    let colId = cell.id
                    let col = df.cols[colId];
                    if (!cell.empty) {
                        if (col.children.length === 0) {
                            th.innerHTML = col.name
                        } else {
                            let link = document.createElement(&quot;a&quot;)
                            link.className = &quot;expander&quot;
                            let that = this
                            link.onclick = function () {
                                col.expanded = !col.expanded
                                that.renderTable(id)
                            }
                            link.appendChild(this.createExpander(col.expanded))
                            link.innerHTML += col.name
                            th.appendChild(link)
                        }
                    }
                    let classes = (cell.leftBd ? &quot; leftBorder&quot; : &quot;&quot;) + (cell.rightBd ? &quot; rightBorder&quot; : &quot;&quot;)
                    if (col.rightAlign)
                        classes += &quot; rightAlign&quot;
                    if (isLastRow)
                        classes += &quot; bottomBorder&quot;
                    if (classes.length &gt; 0)
                        th.setAttribute(&quot;class&quot;, classes)
                    tr.appendChild(th)
                }
            }

            &sol;&sol; body
            let body = document.createElement(&quot;tbody&quot;)
            table.appendChild(body)

            let columns = renderData.pop()
            for (let row = 0; row &lt; df.nrow; row++) {
                let tr = document.createElement(&quot;tr&quot;);
                body.appendChild(tr)
                for (let i = 0; i &lt; columns.length; i++) {
                    let cell = columns[i]
                    let td = document.createElement(&quot;td&quot;);
                    let colId = cell.id
                    let col = df.cols[colId]
                    let classes = (cell.leftBd ? &quot; leftBorder&quot; : &quot;&quot;) + (cell.rightBd ? &quot; rightBorder&quot; : &quot;&quot;)
                    if (col.rightAlign)
                        classes += &quot; rightAlign&quot;
                    if (classes.length &gt; 0)
                        td.setAttribute(&quot;class&quot;, classes)
                    tr.appendChild(td)
                    let value = col.values[row]
                    if (value.frameId !== undefined) {
                        let frameId = value.frameId
                        let expanded = rootDf.expandedFrames.has(frameId)
                        let link = document.createElement(&quot;a&quot;)
                        link.className = &quot;expander&quot;
                        let that = this
                        link.onclick = function () {
                            if (rootDf.expandedFrames.has(frameId))
                                rootDf.expandedFrames.delete(frameId)
                            else rootDf.expandedFrames.add(frameId)
                            that.renderTable(id)
                        }
                        link.appendChild(this.createExpander(expanded))
                        link.innerHTML += value.value
                        if (expanded) {
                            td.appendChild(link)
                            td.appendChild(document.createElement(&quot;p&quot;))
                            const childTable = document.createElement(&quot;table&quot;)
                            childTable.className = &quot;dataframe&quot;
                            childTable.id = &quot;df_&quot; + frameId
                            let childDf = rootDf.childFrames[frameId]
                            childTable.df = childDf
                            td.appendChild(childTable)
                            this.renderTable(frameId)
                            if (childDf.nrow !== childDf.totalRows) {
                                const footer = document.createElement(&quot;p&quot;)
                                footer.innerText = `... showing only top ${childDf.nrow} of ${childDf.totalRows} rows`
                                td.appendChild(footer)
                            }
                        } else {
                            td.appendChild(link)
                        }
                    } else if (value.style !== undefined) {
                        td.innerHTML = value.value
                        td.setAttribute(&quot;style&quot;, value.style)
                    } else td.innerHTML = value
                    this.nodeScriptReplace(td)
                }
            }
        }

        this.nodeScriptReplace = function (node) {
            if (this.nodeScriptIs(node) === true) {
                node.parentNode.replaceChild(this.nodeScriptClone(node), node);
            } else {
                let i = -1, children = node.childNodes;
                while (++i &lt; children.length) {
                    this.nodeScriptReplace(children[i]);
                }
            }

            return node;
        }

        this.nodeScriptClone = function (node) {
            let script = document.createElement(&quot;script&quot;);
            script.text = node.innerHTML;

            let i = -1, attrs = node.attributes, attr;
            while (++i &lt; attrs.length) {
                script.setAttribute((attr = attrs[i]).name, attr.value);
            }
            return script;
        }

        this.nodeScriptIs = function (node) {
            return node.tagName === 'SCRIPT';
        }
    })()

    window.call_DataFrame = function (f) {
        return f();
    };

    let funQueue = window[&quot;kotlinQueues&quot;] &amp;&amp; window[&quot;kotlinQueues&quot;][&quot;DataFrame&quot;];
    if (funQueue) {
        funQueue.forEach(function (f) {
            f();
        });
        funQueue = [];
    }
})()

&sol;*&lt;!--*&sol;
call_DataFrame(function() { DataFrame.addTable({ cols: [{ name: &quot;&lt;span title=&bsol;&quot;untitled: String&bsol;&quot;&gt;untitled&lt;&sol;span&gt;&quot;, children: [], rightAlign: false, values: [&quot;RescheduledEvent&quot;,&quot;ComponentStateChangeEvent&quot;,&quot;StateChangedEvent&quot;,&quot;ResourceEvent&quot;,&quot;InteractionEvent&quot;] }, 
{ name: &quot;&lt;span title=&bsol;&quot;count: Int&bsol;&quot;&gt;count&lt;&sol;span&gt;&quot;, children: [], rightAlign: true, values: [&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;numbers&bsol;&quot;&gt;2547&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;numbers&bsol;&quot;&gt;860&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;numbers&bsol;&quot;&gt;960&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;numbers&bsol;&quot;&gt;1708&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;,&quot;&lt;span class=&bsol;&quot;formatted&bsol;&quot; title=&bsol;&quot;&bsol;&quot;&gt;&lt;span class=&bsol;&quot;numbers&bsol;&quot;&gt;328&lt;&sol;span&gt;&lt;&sol;span&gt;&quot;] }, 
], id: 16777243, rootId: 16777243, totalRows: 5 } ) });
&sol;*--&gt;*&sol;

call_DataFrame(function() { DataFrame.renderTable(16777243) });


        &lt;&sol;script&gt;
        &lt;&sol;html&gt;"></iframe>
            <script>
                function o_resize_iframe_out_10() {
                    let elem = document.getElementById("iframe_out_10");
                    resize_iframe_out_10(elem);
                    setInterval(resize_iframe_out_10, 5000, elem);
                }
                function resize_iframe_out_10(el) {
                    let h = el.contentWindow.document.body.scrollHeight;
                    el.height = h === 0 ? 0 : h + 41;
                }
            </script>        <html>
        <head>
            <style type="text/css">
                :root {
    --background: #fff;
    --background-odd: #f5f5f5;
    --background-hover: #d9edfd;
    --header-text-color: #474747;
    --text-color: #848484;
    --text-color-dark: #000;
    --text-color-medium: #737373;
    --text-color-pale: #b3b3b3;
    --inner-border-color: #aaa;
    --bold-border-color: #000;
    --link-color: #296eaa;
    --link-color-pale: #296eaa;
    --link-hover: #1a466c;
}

:root[theme="dark"], :root [data-jp-theme-light="false"], .dataframe_dark{
    --background: #303030;
    --background-odd: #3c3c3c;
    --background-hover: #464646;
    --header-text-color: #dddddd;
    --text-color: #b3b3b3;
    --text-color-dark: #dddddd;
    --text-color-medium: #b2b2b2;
    --text-color-pale: #737373;
    --inner-border-color: #707070;
    --bold-border-color: #777777;
    --link-color: #008dc0;
    --link-color-pale: #97e1fb;
    --link-hover: #00688e;
}

p.dataframe_description {
    color: var(--text-color-dark);
}

table.dataframe {
    font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
    font-size: 12px;
    background-color: var(--background);
    color: var(--text-color-dark);
    border: none;
    border-collapse: collapse;
}

table.dataframe th, td {
    padding: 6px;
    border: 1px solid transparent;
    text-align: left;
}

table.dataframe th {
    background-color: var(--background);
    color: var(--header-text-color);
}

table.dataframe td {
    vertical-align: top;
    white-space: nowrap;
}

table.dataframe th.bottomBorder {
    border-bottom-color: var(--bold-border-color);
}

table.dataframe tbody > tr:nth-child(odd) {
    background: var(--background-odd);
}

table.dataframe tbody > tr:nth-child(even) {
    background: var(--background);
}

table.dataframe tbody > tr:hover {
    background: var(--background-hover);
}

table.dataframe a {
    cursor: pointer;
    color: var(--link-color);
    text-decoration: none;
}

table.dataframe tr:hover > td a {
    color: var(--link-color-pale);
}

table.dataframe a:hover {
    color: var(--link-hover);
    text-decoration: underline;
}

table.dataframe img {
    max-width: fit-content;
}

table.dataframe th.complex {
    background-color: var(--background);
    border: 1px solid var(--background);
}

table.dataframe .leftBorder {
    border-left-color: var(--inner-border-color);
}

table.dataframe .rightBorder {
    border-right-color: var(--inner-border-color);
}

table.dataframe .rightAlign {
    text-align: right;
}

table.dataframe .expanderSvg {
    width: 8px;
    height: 8px;
    margin-right: 3px;
}

table.dataframe .expander {
    display: flex;
    align-items: center;
}

/* formatting */

table.dataframe .null {
    color: var(--text-color-pale);
}

table.dataframe .structural {
    color: var(--text-color-medium);
    font-weight: bold;
}

table.dataframe .dataFrameCaption {
    font-weight: bold;
}

table.dataframe .numbers {
    color: var(--text-color-dark);
}

table.dataframe td:hover .formatted .structural, .null {
    color: var(--text-color-dark);
}

table.dataframe tr:hover .formatted .structural, .null {
    color: var(--text-color-dark);
}


            </style>
        </head>
        <body>
            <table class="dataframe" id="static_df_16777244"><thead><tr><th class="bottomBorder" style="text-align:left">untitled</th><th class="bottomBorder" style="text-align:left">count</th></tr></thead><tbody><tr><td  style="vertical-align:top">RescheduledEvent</td><td  style="vertical-align:top">2547</td></tr><tr><td  style="vertical-align:top">ComponentStateChangeEvent</td><td  style="vertical-align:top">860</td></tr><tr><td  style="vertical-align:top">StateChangedEvent</td><td  style="vertical-align:top">960</td></tr><tr><td  style="vertical-align:top">ResourceEvent</td><td  style="vertical-align:top">1708</td></tr><tr><td  style="vertical-align:top">InteractionEvent</td><td  style="vertical-align:top">328</td></tr></tbody></table>
        </body>
        <script>
            document.getElementById("static_df_16777244").style.display = "none";
        </script>
        </html>



The event distribution shows that the Emergency Room model is primarily time-driven, with RescheduledEvents (~ 40%) dominating due to frequent hold() operations for arrivals, severity escalation, setup, and surgery durations. ResourceEvents (~ 27%) indicate significant doctor allocation and release activity, reflecting contention and queue dynamics around limited medical staff. StateChangedEvents and ComponentStateChangeEvents (~ 28% combined) stem from patient severity progression and status updates (e.g., Waiting → InSurgery → Released/Deceased). InteractionEvents (~ 5%) are comparatively low, suggesting relatively simple direct component interactions. Overall, the model’s dynamics are mainly governed by time scheduling and resource competition rather than complex inter-component messaging.



## Conclusion & Summary

This study demonstrates how a complex and highly dynamic environment such as an Emergency Room can be captured, analyzed, and improved using discrete-event simulation with Kalasim. By explicitly modeling patient deterioration, resource constraints, setup dependencies, and policy-driven decision making, we gain transparency into bottlenecks and systemic trade-offs that are difficult to detect in real operations. The simulation highlights how performance is shaped not only by staffing levels, but by intelligent coordination of rooms, doctors, and triage policies. Such models provide a safe environment to experiment with alternative strategies before applying them in practice. Ultimately, data-driven process modeling enables more informed decisions, more efficient resource utilization, and—most importantly—the potential to save more lives through smarter system design.

Disclaimer: The author is not a medical doctor, so please excuse possible inprecsion in wording and lack of ER process understanding. Feel welcome to suggest corrections or improvements.


```kotlin
:classpath
```




    Current classpath (62 paths):
    C:\Users\brandl\AppData\Local\JetBrains\IntelliJIdea2025.3\kotlinNotebook\kernels\0.15.1-761-1\kotlin-jupyter-script-classpath-shadowed-zip_extracted\annotations-13.0.jar
    C:\Users\brandl\AppData\Local\JetBrains\IntelliJIdea2025.3\kotlinNotebook\kernels\0.15.1-761-1\kotlin-jupyter-script-classpath-shadowed-zip_extracted\api-0.15.1-761-1.jar
    C:\Users\brandl\AppData\Local\JetBrains\IntelliJIdea2025.3\kotlinNotebook\kernels\0.15.1-761-1\kotlin-jupyter-script-classpath-shadowed-zip_extracted\kotlin-reflect-2.2.20.jar
    C:\Users\brandl\AppData\Local\JetBrains\IntelliJIdea2025.3\kotlinNotebook\kernels\0.15.1-761-1\kotlin-jupyter-script-classpath-shadowed-zip_extracted\kotlin-script-runtime-2.2.20.jar
    C:\Users\brandl\AppData\Local\JetBrains\IntelliJIdea2025.3\kotlinNotebook\kernels\0.15.1-761-1\kotlin-jupyter-script-classpath-shadowed-zip_extracted\kotlin-stdlib-2.2.20.jar
    C:\Users\brandl\AppData\Local\JetBrains\IntelliJIdea2025.3\kotlinNotebook\kernels\0.15.1-761-1\kotlin-jupyter-script-classpath-shadowed-zip_extracted\kotlinx-serialization-core-jvm-1.9.0.jar
    C:\Users\brandl\AppData\Local\JetBrains\IntelliJIdea2025.3\kotlinNotebook\kernels\0.15.1-761-1\kotlin-jupyter-script-classpath-shadowed-zip_extracted\kotlinx-serialization-json-jvm-1.9.0.jar
    C:\Users\brandl\AppData\Local\JetBrains\IntelliJIdea2025.3\kotlinNotebook\kernels\0.15.1-761-1\kotlin-jupyter-script-classpath-shadowed-zip_extracted\lib-0.15.1-761-1.jar
    C:\Users\brandl\AppData\Local\JetBrains\IntelliJIdea2025.3\kotlinNotebook\kernels\0.15.1-761-1\kotlin-jupyter-script-classpath-shadowed-zip_extracted\protocol-api-0.15.1-761-1.jar
    C:\Users\brandl\AppData\Local\JetBrains\IntelliJIdea2025.3\kotlinNotebook\kernels\0.15.1-761-1\kotlin-jupyter-script-classpath-shadowed-zip_extracted\slf4j-api-2.0.17.jar
    D:\projects\scheduling\kalasim\modules\notebook\out\production\classes
    D:\projects\scheduling\kalasim\modules\kravis\out\production\classes
    D:\projects\scheduling\kalasim\modules\letsplot\out\production\classes
    D:\projects\scheduling\kalasim\out\production\classes
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlinx\kotlin-jupyter-api\0.16.0-742\62556f34ea4e4f5b738971029511aeca7f794b76\kotlin-jupyter-api-0.16.0-742.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin\kotlin-stdlib\2.2.21\fa374a986e128314c3db00a20aae55f72a258511\kotlin-stdlib-2.2.21.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\com.github.holgerbrandl\kravis\1.0.4\6d35511d9da3aee1247be5219be132e9da85f2b8\kravis-1.0.4.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.lets-plot\lets-plot-kotlin-jvm\4.7.0\fe0289372c3e91f4a4f5c44fa67c96194b224c42\plot-api-jvm-4.7.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\io.github.oshai\kotlin-logging-jvm\7.0.14\df9cdbd614154cc886a255e23b87ce165c9e91c6\kotlin-logging-jvm-7.0.14.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\com.github.holgerbrandl\kdfutils\1.5.0\c30a96b7f4e76d3eb327271f5f09d2138641c9a\kdfutils-1.5.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\com.github.holgerbrandl\jsonbuilder\0.10\d77bc13c17047e059d0840a27146e79cc017c1f7\jsonbuilder-0.10.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.apache.commons\commons-math3\3.6.1\e4ba98f1d4b3c80ec46392f25e094a6a2e58fcbf\commons-math3-3.6.1.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.json\json\20251224\758adfe5ff5c3cc286d62c9d097cbca84f870536\json-20251224.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.slf4j\slf4j-api\2.0.17\d9e58ac9c7779ba3bf8142aff6c830617a7fe60f\slf4j-api-2.0.17.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlinx\kotlin-jupyter-protocol-api\0.16.0-742\1f6e418d14bfc7fe3beed85143f4317e35fa0b65\kotlin-jupyter-protocol-api-0.16.0-742.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains\annotations\23.0.0\8cc20c07506ec18e0834947b84a864bfc094484e\annotations-23.0.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlinx\dataframe-core\0.15.0\75c96920a6eaf73af438b9de44f992f5fd16ee\dataframe-core-0.15.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.lets-plot\lets-plot-common\4.3.0\45655e7411f409754e61ff6321815317130f83d4\lets-plot-common-4.3.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\io.insert-koin\koin-core-jvm\4.1.0\fa2551d34ac9aa9e698490c0fa73585afdf75353\koin-core-jvm-4.1.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin\kotlin-stdlib-jdk8\1.8.0\ed04f49e186a116753ad70d34f0ac2925d1d8020\kotlin-stdlib-jdk8-1.8.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlinx\kotlinx-coroutines-core-jvm\1.10.2\4a9f78ef49483748e2c129f3d124b8fa249dafbf\kotlinx-coroutines-core-jvm-1.10.2.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlinx\kotlinx-datetime-jvm\0.6.2\f177b43ce53f69151797206ce762b36199692474\kotlinx-datetime-jvm-0.6.2.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlinx\kotlinx-serialization-json-jvm\1.9.0\aea6f7d49fe5c458f8963ee6d4bdaf4a459ab3e7\kotlinx-serialization-json-jvm-1.9.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin\kotlin-reflect\2.0.20\580c610641d75b6448825cbe455a1ded220e148b\kotlin-reflect-2.0.20.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.apache.commons\commons-csv\1.12.0\c77e053d7189bc0857f8d323ab61cb949965fbd1\commons-csv-1.12.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.lets-plot\commons-jvm\4.3.0\f4495dd6291dd2c77250dda6c6adda72b92f045d\commons-jvm-4.3.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.lets-plot\datamodel-jvm\4.3.0\e7954fefada53989c04e2077f2d990822935e993\datamodel-jvm-4.3.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.lets-plot\plot-base-jvm\4.3.0\14dfe53a529efca3810c3c0969f5415813082758\plot-base-jvm-4.3.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.lets-plot\plot-builder-jvm\4.3.0\f1c585ab0474ff00edeeb136b8dd697ee5ddee8c\plot-builder-jvm-4.3.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.lets-plot\plot-stem-jvm\4.3.0\64f6a9f7ce1013bd10e65dcabba1acc720af567c\plot-stem-jvm-4.3.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.lets-plot\deprecated-in-v4-jvm\4.3.0\f7c1edd0d59b46ad0b4576b352eeb56f7a694e90\deprecated-in-v4-jvm-4.3.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin\kotlin-stdlib-jdk7\1.8.0\3c91271347f678c239607abb676d4032a7898427\kotlin-stdlib-jdk7-1.8.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\commons-io\commons-io\2.17.0\ddcc8433eb019fb48fe25207c0278143f3e1d7e2\commons-io-2.17.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\commons-codec\commons-codec\1.17.1\973638b7149d333563584137ebf13a691bb60579\commons-codec-1.17.1.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\io.github.microutils\kotlin-logging-jvm\2.0.5\a2b9216b3958c6d70a5f3842d851f224eba3818c\kotlin-logging-jvm-2.0.5.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlinx\kotlinx-serialization-core-jvm\1.9.0\91448df39c558f7c6147b8bd8db01debe16e0cc1\kotlinx-serialization-core-jvm-1.9.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\com.google.code.gson\gson\2.13.2\48b8230771e573b54ce6e867a9001e75977fe78e\gson-2.13.2.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.rosuda.REngine\Rserve\1.8.1\c21c2bedcdca530edbd2acdbd0bb4d99646920d6\Rserve-1.8.1.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.rosuda.REngine\REngine\2.1.0\73c31209d4ac42d669ccf731e8a1d845f601adac\REngine-2.1.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin\kotlin-stdlib-jdk8\2.0.20\764dd55ea8b14f76a3d66c403cfed212287a23dd\kotlin-stdlib-jdk8-2.0.20.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\com.google.errorprone\error_prone_annotations\2.41.0\4381275efdef6ddfae38f002c31e84cd001c97f0\error_prone_annotations-2.41.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\commons-io\commons-io\2.18.0\44084ef756763795b31c578403dd028ff4a22950\commons-io-2.18.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\ch.randelshofer\fastdoubleparser\2.0.1\ccf4a1c0441e7af5cf24c7b3a9766b2e2646bdea\fastdoubleparser-2.0.1.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.slf4j\slf4j-api\2.0.16\172931663a09a1fa515567af5fbef00897d3c04\slf4j-api-2.0.16.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin\kotlin-stdlib-jdk7\2.0.20\d70dafddc862404c4ecdb59eeb4bdfac8019937d\kotlin-stdlib-jdk7-2.0.20.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\com.squareup\kotlinpoet-jvm\1.18.1\c4143e9d675aac41f47ceb4e7886d801010bd0c8\kotlinpoet-jvm-1.18.1.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlinx\kotlinx-serialization-json-jvm\1.7.1\c2a4c17f9246285d862cbe1f03d6f8e0848d1958\kotlinx-serialization-json-jvm-1.7.1.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlinx\kotlinx-serialization-core-jvm\1.7.1\f58a5f5e8ae06a468b523490a7085b781af4a6b3\kotlinx-serialization-core-jvm-1.7.1.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\co.touchlab\stately-concurrent-collections-jvm\2.1.0\5c03b644537d7926a8f77e5735a8bebb55dafdcd\stately-concurrent-collections-jvm-2.1.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\co.touchlab\stately-concurrency-jvm\2.1.0\6285428408c4d7e4a6d9a09511de877103effd81\stately-concurrency-jvm-2.1.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\co.touchlab\stately-strict-jvm\2.1.0\3ae8369209065455f4630793bf6aca0fb88c8b6b\stately-strict-jvm-2.1.0.jar
    C:\Users\brandl\.gradle\caches\modules-2\files-2.1\org.slf4j\slf4j-api\1.7.29\e56bf4473a4c6b71c7dd397a833dce86d1993d9d\slf4j-api-1.7.29.jar


