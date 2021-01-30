# cd /c/brandl_data/projects/scheduling/kalasim/simulations/simmer/
# rend.R simmer_plot.R

pacman::p_load(simmer)
pacman::p_load(simmer.plot)
pacman::p_load(tidyverse)

t0 <- trajectory("my trajectory") %>%
## add an intake activity
    seize("nurse", 1) %>%
    timeout(function() rnorm(1, 15)) %>%
    release("nurse", 1) %>%
## add a consultation activity
    seize("doctor", 1) %>%
    timeout(function() rnorm(1, 20)) %>%
    release("doctor", 1) %>%
## add a planning activity
    seize("administration", 1) %>%
    timeout(function() rnorm(1, 5)) %>%
    release("administration", 1)

env <- simmer("SuperDuperSim") %>%
    add_resource("nurse", 1) %>%
    add_resource("doctor", 2) %>%
    add_resource("administration", 1) %>%
    add_generator("patient", t0, function() rnorm(1, 10, 2)) %>%
    run(until=80)

resources <- get_mon_resources(env) %>% tbl_df
arrivals <- get_mon_arrivals(env) %>% tbl_df

plot(resources, metric="usage", "doctor", items = "server", steps = TRUE)

plot(resources, metric="utilization", c("nurse", "doctor", "administration"))


plot(arrivals, metric="waiting_time")