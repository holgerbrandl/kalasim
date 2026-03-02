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




    org.kalasim.EventLog@12a886a



Now run it for some days, and print its state afterwards


```kotlin
er.run(7.days)

// print the object to render short status summary
er
```




    EmergencyRoom Summary Statistics (at t=2022-03-02T06:00:00Z):
      Physicians: 8
      Rooms: 4
      Waiting Area Capacity: 300
      
      Incoming Patients: 344
      Currently Waiting: 12
      Treated Successfully: 291
      Deceased: 37
      Success Rate: 88,72%
      
      Nurse Policy: FifoNurse




```kotlin
er.waitingLine.sizeTimeline.display("Waiting Time")
```




    
![jpeg](emergency_room_files/emergency_room_7_0.jpg)
    




```kotlin
er.treatedMonitor.display("Treated Patients")
```




    
![jpeg](emergency_room_files/emergency_room_8_0.jpg)
    



Thats visualization is technically correct, but most likely we rather want to see treated patients per day



```kotlin
import org.kalasim.plot.kravis.displayStateCounts

er.patients.map{it.severity}.displayStateCounts()
```




    
![jpeg](emergency_room_files/emergency_room_10_0.jpg)
    




```kotlin
 er.deceasedMonitor.display("Deceased Patients")
```




    
![jpeg](emergency_room_files/emergency_room_11_0.jpg)
    





## Analysis

To analyze the model, we first use different visualization functinos defined for collection os [states](https://www.kalasim.org/state/), [components](https://www.kalasim.org/component/) and [resources](https://www.kalasim.org/resource/).


```kotlin
import org.kalasim.plot.kravis.displayTimelines

er.doctors.displayTimelines()

```




    
![jpeg](emergency_room_files/emergency_room_14_0.jpg)
    




```kotlin
er.doctors.displayTimelines(byRequester = true, colorBy = { it.requester.name})
```




    
![jpeg](emergency_room_files/emergency_room_15_0.jpg)
    



When studying the data from above, we observe that  an ER with more staff than available surgery rooms (e.g., `numPhysicians = 8` but only a limited number of rooms as defined in the `EmergencyRoom` example), we observe that the bottleneck shifts from staff availability to room availability.



```kotlin
import org.kalasim.plot.kravis.displayStateCounts

er.patients.map { it.patientStatus }.displayStateCounts()

```




    
![jpeg](emergency_room_files/emergency_room_17_0.jpg)
    



Daily statistics vary slightly, but the overall distribution remains stable. This is expected since the current model does not yet include time-varying factors such as weekend shifts or staff changes.



```kotlin
er.patients.take(20).map { it.patientStatus }.displayTimelines(to = er.startDate + 4.hours)

```




    
![jpeg](emergency_room_files/emergency_room_19_0.jpg)
    



We observe the different arrivals and their stay durations. Most of the sampled patients in the observed time window are going into surgey without a previous waiting time.


```kotlin
import org.kalasim.plot.kravis.displayStayDistributions

er.patients.map{ it.patientStatus }.displayStayDistributions()

```




    
![jpeg](emergency_room_files/emergency_room_21_0.jpg)
    



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

intDF.take(10).toHTML()
```




            <iframe onload="o_resize_iframe_out_8()" style="width:100%;" class="result_container" id="iframe_out_8" frameBorder="0" srcdoc="        &lt;html&gt;
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
            &lt;table class=&quot;dataframe&quot; id=&quot;df_16777239&quot;&gt;&lt;&sol;table&gt;

&lt;table class=&quot;dataframe&quot; id=&quot;static_df_16777240&quot;&gt;&lt;thead&gt;&lt;tr&gt;&lt;th class=&quot;bottomBorder&quot; style=&quot;text-align:left&quot;&gt;time&lt;&sol;th&gt;&lt;th class=&quot;bottomBorder&quot; style=&quot;text-align:left&quot;&gt;current&lt;&sol;th&gt;&lt;th class=&quot;bottomBorder&quot; style=&quot;text-align:left&quot;&gt;component&lt;&sol;th&gt;&lt;th class=&quot;bottomBorder&quot; style=&quot;text-align:left&quot;&gt;actionFn&lt;&sol;th&gt;&lt;th class=&quot;bottomBorder&quot; style=&quot;text-align:left&quot;&gt;action&lt;&sol;th&gt;&lt;th class=&quot;bottomBorder&quot; style=&quot;text-align:left&quot;&gt;eventType&lt;&sol;th&gt;&lt;th class=&quot;bottomBorder&quot; style=&quot;text-align:left&quot;&gt;tickTime&lt;&sol;th&gt;&lt;&sol;tr&gt;&lt;&sol;thead&gt;&lt;tbody&gt;&lt;tr&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;2022-02-23T06:00:00Z&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;null&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;main&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;null&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;running; Hold +168.00, scheduled for &lt;span class=&quot;structural&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;RescheduledEvent&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;1645596000&lt;&sol;td&gt;&lt;&sol;tr&gt;&lt;tr&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;2022-02-23T06:00:00Z&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;room 0&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;room 0&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;org.kalasim.analysis.EventsKt$$Lambda&lt;span class=&quot;structural&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;canceled&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;ComponentStateChangeEvent&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;1645596000&lt;&sol;td&gt;&lt;&sol;tr&gt;&lt;tr&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;2022-02-23T06:00:00Z&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;room 1&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;room 1&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;org.kalasim.analysis.EventsKt$$Lambda&lt;span class=&quot;structural&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;canceled&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;ComponentStateChangeEvent&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;1645596000&lt;&sol;td&gt;&lt;&sol;tr&gt;&lt;tr&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;2022-02-23T06:00:00Z&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;room 2&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;room 2&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;org.kalasim.analysis.EventsKt$$Lambda&lt;span class=&quot;structural&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;canceled&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;ComponentStateChangeEvent&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;1645596000&lt;&sol;td&gt;&lt;&sol;tr&gt;&lt;tr&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;2022-02-23T06:00:00Z&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;room 3&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;room 3&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;org.kalasim.analysis.EventsKt$$Lambda&lt;span class=&quot;structural&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;canceled&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;ComponentStateChangeEvent&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;1645596000&lt;&sol;td&gt;&lt;&sol;tr&gt;&lt;tr&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;2022-02-23T06:00:00Z&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;ComponentGenerator.1&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;ComponentGenerator.1&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;null&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;Hold +.08, scheduled for .08&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;RescheduledEvent&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;1645596000&lt;&sol;td&gt;&lt;&sol;tr&gt;&lt;tr&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;2022-02-23T06:04:57.988113230Z&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;ComponentGenerator.1&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;Patient(fullName=Armando Kessler, pat&lt;span class=&quot;structural&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;null&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;Activated, scheduled for .08&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;RescheduledEvent&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;1645596297&lt;&sol;td&gt;&lt;&sol;tr&gt;&lt;tr&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;2022-02-23T06:04:57.988113230Z&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;ComponentGenerator.1&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;Patient(fullName=Armando Kessler, pat&lt;span class=&quot;structural&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;org.kalasim.analysis.EventsKt$$Lambda&lt;span class=&quot;structural&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;canceled&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;ComponentStateChangeEvent&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;1645596297&lt;&sol;td&gt;&lt;&sol;tr&gt;&lt;tr&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;2022-02-23T06:04:57.988113230Z&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;ComponentGenerator.1&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;ComponentGenerator.1&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;null&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;Hold +.20, scheduled for .28&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;RescheduledEvent&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;1645596297&lt;&sol;td&gt;&lt;&sol;tr&gt;&lt;tr&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;2022-02-23T06:16:54.513625724Z&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;ComponentGenerator.1&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;Patient(fullName=Laurie Keebler, pati&lt;span class=&quot;structural&quot;&gt;...&lt;&sol;span&gt;&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;null&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;Activated, scheduled for .28&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;RescheduledEvent&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;1645597014&lt;&sol;td&gt;&lt;&sol;tr&gt;&lt;&sol;tbody&gt;&lt;&sol;table&gt;
&lt;p class=&quot;dataframe_description&quot;&gt;DataFrame [10 x 7]&lt;&sol;p&gt;

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
], id: 16777239, rootId: 16777239, totalRows: 10 } ) });
&sol;*--&gt;*&sol;

call_DataFrame(function() { DataFrame.renderTable(16777239) });

document.getElementById(&quot;static_df_16777240&quot;).style.display = &quot;none&quot;;

        &lt;&sol;script&gt;
        &lt;&sol;html&gt;"></iframe>
            <script>
                function o_resize_iframe_out_8() {
                    let elem = document.getElementById("iframe_out_8");
                    resize_iframe_out_8(elem);
                    setInterval(resize_iframe_out_8, 5000, elem);
                }
                function resize_iframe_out_8(el) {
                    let h = el.contentWindow.document.body.scrollHeight;
                    el.height = h === 0 ? 0 : h + 41;
                }
            </script>




```kotlin
intDF    .groupBy {  expr { eventType } }
    .count()
    .toHTML()
```




            <iframe onload="o_resize_iframe_out_6()" style="width:100%;" class="result_container" id="iframe_out_6" frameBorder="0" srcdoc="        &lt;html&gt;
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
            &lt;table class=&quot;dataframe&quot; id=&quot;df_16777235&quot;&gt;&lt;&sol;table&gt;

&lt;table class=&quot;dataframe&quot; id=&quot;static_df_16777236&quot;&gt;&lt;thead&gt;&lt;tr&gt;&lt;th class=&quot;bottomBorder&quot; style=&quot;text-align:left&quot;&gt;untitled&lt;&sol;th&gt;&lt;th class=&quot;bottomBorder&quot; style=&quot;text-align:left&quot;&gt;count&lt;&sol;th&gt;&lt;&sol;tr&gt;&lt;&sol;thead&gt;&lt;tbody&gt;&lt;tr&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;RescheduledEvent&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;2547&lt;&sol;td&gt;&lt;&sol;tr&gt;&lt;tr&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;ComponentStateChangeEvent&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;860&lt;&sol;td&gt;&lt;&sol;tr&gt;&lt;tr&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;StateChangedEvent&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;960&lt;&sol;td&gt;&lt;&sol;tr&gt;&lt;tr&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;ResourceEvent&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;1708&lt;&sol;td&gt;&lt;&sol;tr&gt;&lt;tr&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;InteractionEvent&lt;&sol;td&gt;&lt;td  style=&quot;vertical-align:top&quot;&gt;328&lt;&sol;td&gt;&lt;&sol;tr&gt;&lt;&sol;tbody&gt;&lt;&sol;table&gt;
&lt;p class=&quot;dataframe_description&quot;&gt;DataFrame [5 x 2]&lt;&sol;p&gt;

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
], id: 16777235, rootId: 16777235, totalRows: 5 } ) });
&sol;*--&gt;*&sol;

call_DataFrame(function() { DataFrame.renderTable(16777235) });

document.getElementById(&quot;static_df_16777236&quot;).style.display = &quot;none&quot;;

        &lt;&sol;script&gt;
        &lt;&sol;html&gt;"></iframe>
            <script>
                function o_resize_iframe_out_6() {
                    let elem = document.getElementById("iframe_out_6");
                    resize_iframe_out_6(elem);
                    setInterval(resize_iframe_out_6, 5000, elem);
                }
                function resize_iframe_out_6(el) {
                    let h = el.contentWindow.document.body.scrollHeight;
                    el.height = h === 0 ? 0 : h + 41;
                }
            </script>



The event distribution shows that the Emergency Room model is primarily time-driven, with RescheduledEvents (~ 40%) dominating due to frequent hold() operations for arrivals, severity escalation, setup, and surgery durations. ResourceEvents (~ 27%) indicate significant doctor allocation and release activity, reflecting contention and queue dynamics around limited medical staff. StateChangedEvents and ComponentStateChangeEvents (~ 28% combined) stem from patient severity progression and status updates (e.g., Waiting → InSurgery → Released/Deceased). InteractionEvents (~ 5%) are comparatively low, suggesting relatively simple direct component interactions. Overall, the model’s dynamics are mainly governed by time scheduling and resource competition rather than complex inter-component messaging.



## Conclusion & Summary

This study demonstrates how a complex and highly dynamic environment such as an Emergency Room can be captured, analyzed, and improved using discrete-event simulation with Kalasim. By explicitly modeling patient deterioration, resource constraints, setup dependencies, and policy-driven decision making, we gain transparency into bottlenecks and systemic trade-offs that are difficult to detect in real operations. The simulation highlights how performance is shaped not only by staffing levels, but by intelligent coordination of rooms, doctors, and triage policies. Such models provide a safe environment to experiment with alternative strategies before applying them in practice. Ultimately, data-driven process modeling enables more informed decisions, more efficient resource utilization, and—most importantly—the potential to save more lives through smarter system design.

Disclaimer: The author is not a medical doctor, so please excuse possible inprecsion in wording and lack of ER process understanding. Feel welcome to suggest corrections or improvements.
