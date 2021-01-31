# cd /c/brandl_data/projects/scheduling/kalasim/simulations/simmer/
# rend.R simmer_plot.R

pacman::p_load(simmer)
pacman::p_load(simmer.plot)
pacman::p_load(tidyverse)

patient <- trajectory("patient trajectory") %>%
## add an intake activity
    seize("nurse", 1) %>%
    timeout(function() rnorm(1, 15)) %>%
    release("nurse", 1) %>%
## add a consultation activity
    seize("doctor", 1) %>%
    timeout(function() rnorm(1, 20)) %>%
    release("doctor", 1) %>%
## add a planning activity
    # timeout(function() rnorm(1, 5)) %>%
    seize("administration", 1) %>%
    timeout(function() rnorm(1, 5)) %>%
    release("administration", 1)

env <- simmer("SuperDuperSim") %>%
    add_resource("nurse", 1) %>%
    add_resource("doctor", 2) %>%
    add_resource("administration", 1) %>%
    add_generator("patient", patient, function() rnorm(1, 10, 2)) %>%
    run(until = 80)

resources <- get_mon_resources(env) #%>% tbl_df
arrivals <- get_mon_arrivals(env) #%>% tbl_df


# The S3 method for 'resources' provides two metrics: "usage" and "utilization".
# The "usage" metric shows a line graph of the cumulative average resource usage throughout the simulation, for each resource, replication and item (by default, queue, server and system, which is the sum of queue and server).
#
# If steps=TRUE, a stairstep graph with the instantaneous values is provided instead.
#
# The "utilization" metric shows a bar plot of the average resource utilization (total time in use divided by the total simulation time). For multiple replications, the bar represents the median, and the error bars represent the quartiles. Thus, if a single replication is provided, the bar and the error bar coincide.

resources %>% head
plot(resources, metric = "usage", "doctor", items = "server", steps = TRUE)

plot(resources, metric = "utilization", c("nurse", "doctor", "administration"))

## experiments
plot(resources, metric = "usage", "doctor", items = "server", steps = TRUE)
plot(resources, metric = "usage", "doctor", steps = TRUE)
plot(resources, metric = "usage", steps = TRUE)
plot(resources, metric = "usage")
plot(resources, metric = "utilization") # average resource utilization (total time in use divided by the total simulation time)

get_mon_arrivals(env, per_resource=TRUE) %>% head
get_mon_arrivals(env, per_resource=TRUE) %>% filter(name=="patient0")
get_mon_arrivals(env) %>% head



# The S3 method for 'arrivals' provides three metrics: "activity_time", "waiting_time", and "flow_time". The "activity_time" is the amount of time spent in active state (i.e., in timeout activities), and it is already provided in the output of get_mon_arrivals. The "flow_time" is the amount of time spent in the system, and it is computed as follows: flow = end_time - start_time. Finally, the "waiting_time" is the amount of time spent waiting (e.g., in resources' queues, or due to a wait activity...), and it is computed as follows: waiting_time = flow_time - activity_time. This method does not apply any summary, but just shows a line plot of the values throughout the simulation.
plot(arrivals, metric = "waiting_time")

##
## same with replicas
##

# from https://cran.r-project.org/web/packages/simmer/vignettes/simmer-01-introduction.html#replication

pacman::p_load(parallel)

envs <- mclapply(1 : 100, function(i) {
    simmer("SuperDuperSim") %>%
        add_resource("nurse", 1) %>%
        add_resource("doctor", 2) %>%
        add_resource("administration", 1) %>%
        add_generator("patient", patient, function() rnorm(1, 10, 2)) %>%
        run(80) %>%
        wrap()
})

envs %>%
get_mon_resources() %>%
head()


envs %>% get_mon_resources() %>% plot( metric = "utilization")
envs %>% get_mon_resources() %>% plot( metric = "usage")


envs %>%
get_mon_arrivals() %>%
head()