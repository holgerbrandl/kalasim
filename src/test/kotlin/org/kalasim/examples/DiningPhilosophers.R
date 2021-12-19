## From https://r-simmer.org/articles/simmer-08-philosophers.html

pacman::p_load(simmer)
pacman::p_load(simmer.plot)
require(tidyverse)

simulate <- function(fork_seq, time, thinking = function() rexp(1, 1), eating = function() rexp(1, 1), lag = 0.1, seed = 313) {
  set.seed(seed)

  env <- simmer("Dining philosophers")

  for (i in seq_along(fork_seq)) {
    philosopher <- names(fork_seq)[[i]]
    forks <- paste0("fork_", fork_seq[[i]])

    dining <- trajectory() %>%
      timeout(thinking) %>%
      seize(forks[[1]]) %>%
      timeout(lag) %>%
      seize(forks[[2]]) %>%
      timeout(eating) %>%
      release(forks[[1]]) %>%
      release(forks[[2]]) %>%
      rollback(7) # back to think

    env %>%
      add_resource(paste0("fork_", i)) %>%
      add_generator(paste0(philosopher, "_"), dining, at(0))
  }

  run(env, time)
}


## visualize with gantt
states <- c("hungry", "eating")

env %>%
  get_mon_arrivals(per_resource = TRUE) %>%
  tbl_df

philosophers_gantt <- function(env, size = 15) env %>%
  get_mon_arrivals(per_resource = TRUE) %>%
  transform(
    philosopher = sub("_[0-9]*", "", name),
    state = factor(states, states)
  ) %>%
  ggplot(aes(y = philosopher, yend = philosopher)) +
  xlab("time") +
  geom_segment(aes(x = start_time, xend = end_time, color = state), size = size)

fork_seq <- list(
  Socrates = c(1, 2),
  Pythagoras = c(2, 3),
  Plato = c(3, 4),
  Aristotle = c(4, 1)
)

simulate(fork_seq, time = 50) %>%
  print() %>%
  philosophers_gantt() + theme_bw()


env = simulate(fork_seq, time = 50)


## Step by step
env
# simmer environment: Dining philosophers | now: 50 | next: 50.8360142024587
# { Monitor: in memory }
# { Resource: fork_1 | monitored: TRUE | server status: 1(1) | queue status: 0(Inf) }
# { Resource: fork_2 | monitored: TRUE | server status: 1(1) | queue status: 1(Inf) }
# { Resource: fork_3 | monitored: TRUE | server status: 1(1) | queue status: 0(Inf) }
# { Resource: fork_4 | monitored: TRUE | server status: 1(1) | queue status: 0(Inf) }
# { Source: Socrates_ | monitored: 1 | n_generated: 1 }
# { Source: Pythagoras_ | monitored: 1 | n_generated: 1 }
# { Source: Plato_ | monitored: 1 | n_generated: 1 }
# { Source: Aristotle_ | monitored: 1 | n_generated: 1 }

# We find:
# server status == claimed
# queue status == requests

env %>% get_mon_arrivals() %>% tbl_df
env %>%
  get_mon_arrivals(per_resource = TRUE) %>%
  transform(
    philosopher = sub("_[0-9]*", "", name),
    state = factor(states, states)
  ) %>%
  tbl_df
# # A tibble: 116 x 6
# name         start_time end_time activity_time resource replication
# <chr>             <dbl>    <dbl>         <dbl> <chr>          <int>
# 1 Pythagoras_0      0.143    0.461         0.319 fork_2             1
# 2 Pythagoras_0      0.243    0.461         0.219 fork_3             1
# 3 Pythagoras_0      1.38     2.40          1.01  fork_2             1
# 4 Pythagoras_0      1.48     2.40          0.915 fork_3             1

# # We find:
# replication == quantity

env %>% get_mon_attributes
env %>% get_mon_resources %>% tbl_df

fork_seq$Aristotle <- rev(fork_seq$Aristotle)

fork_seq$Aristotle <- rev(fork_seq$Aristotle)

simulate(fork_seq, time = 50) %>%
  print() %>%
  philosophers_gantt() + theme_bw()

env %>% philosophers_gantt() + theme_bw()