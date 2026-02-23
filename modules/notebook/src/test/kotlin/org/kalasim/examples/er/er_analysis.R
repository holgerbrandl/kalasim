#' ---
#' title: "Emergency Room Report"
#' format:
#'   html:
#'     toc: true
#'     toc-depth: 3      # Levels of headings to include
#'     toc-title: "Contents"
#'     toc-location: left
#'     self-contained: true
#'     page-layout: full
#' lightbox: true
#' fig-width: 16
#' fig-height: 11
#' max-width: 110%
#' width: 100
#' engine: knitr
#' execute:
#'   cache: false  # Disable caching to always pull fresh data
#' ---
#'
#' <style>
#' .main-container {
#' max-width: 1000px !important;
#' }
#' </style>
#'

#+ echo=FALSE
#########################LOADING AND PREPROCESSING########################################################
#### Load core libs
suppressPackageStartupMessages({
  if (!require("pacman")) install.packages("pacman")
})

pacman::p_load(tidyverse, scales, grid, magrittr, readxl, digest, snakecase, lubridate)
pacman::p_load(svglite)
pacman::p_load_gh("holgerbrandl/datautils")
pacman::p_load(patchwork)
pacman::p_load(plotly)

#### Style
#' <style> .main-container { max-width: 1800px !important; } </style>

#+ echo=FALSE
# Display options of plots/figures

knitr::opts_chunk$set(echo = FALSE, fig.width = 12, fig.height = 8)

# https://github.com/tidyverse/ggplot2/issues/5249
update_geom_defaults("tile", list(fill = "grey35"))

#### Loading data

bookings_col_types = list(
  requested = "T",
  honored = "T",
  released = "T",
  requester = "c",
  resource = "c",
  activity = "c",
  quantity = "d",
  eventType = "c",
  tickTime = "d",
  time = "T"
)

bookings = read_csv("er.bookings.csv", show_col_types = FALSE, col_types = bookings_col_types) %>%
  mutate(
    duration = as.numeric(difftime(released, honored, units = "mins"))
  )


#### Processing

# Configure reduction of completion time to add visual spacing between tasks
GANT_SPACER = 800
GANT_SIZE = 4
GANT_SMALL = 2

# Define schedule start and end
schedule_start = min(bookings$honored, na.rm = TRUE)
schedule_end = max(bookings$released, na.rm = TRUE)

#### Visualization

#' # Emergency Room Resource Bookings
#' ## Data Overview

#+ echo=FALSE
bookings %>% head(1000) %>% table_browser("Resource Bookings")

#' ## Medical Staff Schedule

bookings %>%
  ggplot() +
  geom_segment(aes(y = resource, yend = resource, x = honored, xend = released, color = requester), linewidth = GANT_SIZE) +
  geom_vline(aes(xintercept = schedule_start), linetype = "dashed", alpha = 0.5) +
  geom_text(aes(x = schedule_start, y = 0, label = "Schedule Start", vjust = -1.5, hjust = -1, angle = 90)) +
  ggtitle("Medical Staff Schedule") +
  labs(x = "Time", y = "Resource", color = "Requester") +
  theme(legend.position = "top", legend.justification = "right")

#' ## Room Utilization

bookings %>%
  ggplot() +
  geom_segment(aes(y = requester, yend = requester, x = honored, xend = released, color = resource), linewidth = GANT_SIZE) +
  geom_vline(aes(xintercept = schedule_start), linetype = "dashed", alpha = 0.5) +
  geom_text(aes(x = schedule_start, y = 0, label = "Schedule Start", vjust = -1.5, hjust = -1, angle = 90)) +
  ggtitle("Room Utilization") +
  labs(x = "Time", y = "Requester", color = "Resource") +
  theme(legend.position = "top", legend.justification = "right")

#' ## Duration Analysis

bookings %>%
  ggplot(aes(x = duration)) +
  geom_histogram(bins = 30, alpha = 0.7, position = "identity") +
  labs(x = "Duration (minutes)", y = "Count", title = "Distribution of Booking Durations by Resource") +
  theme(legend.position = "top")

#' ## Resource Utilization

bookings %>%
  select(resource, honored, released) %>%
  pivot_longer(cols = c(honored, released), names_to = "event", values_to = "time") %>%
  arrange(resource, time) %>%
  group_by(resource) %>%
  mutate(load = cumsum(if_else(event == "honored", 1, -1))) %>%
  ggplot(aes(time, load, color = resource)) +
  geom_step(linewidth = 1) +
  ggtitle("Resource Load Over Time") +
  labs(x = "Time", y = "Number of Active Bookings", color = "Resource") +
  theme(legend.position = "top")

#' # Component Interactions Analysis


interactions_col_types = list(
  time = "T",
  current = "c",
  component = "c",
  action = "c",
  eventType = "c",
  tickTime = "d"
)


interactions = read_csv("er.interactions.csv", show_col_types = FALSE, col_types = interactions_col_types)

#' ## Interactions Data Overview

#+ echo=FALSE
interactions %>% head(1000) %>% table_browser("Component Interactions")

#' ## Event Type Distribution

interactions %>%
  count(eventType) %>%
  ggplot(aes(x = reorder(eventType, n), y = n, fill = eventType)) +
  geom_col() +
  coord_flip() +
  ggtitle("Distribution of Event Types") +
  labs(x = "Event Type", y = "Count") +
  theme(legend.position = "none")

#' ## Component Activity Timeline

interactions %>%
  filter(!is.na(component) & component != "") %>%
  ggplot() +
  geom_point(aes(x = time, y = component, color = eventType), size = 2, alpha = 0.6) +
  ggtitle("Component Activity Timeline") +
  labs(x = "Time", y = "Component", color = "Event Type") +
  theme(legend.position = "top")

#' ## Component State Changes

interactions %>%
  filter(!is.na(component) & component != "") %>%
  ggplot() +
  geom_segment(aes(x = time, xend = time, y = component, yend = component, color = eventType), linewidth = 1, alpha = 0.7) +
  ggtitle("Component State Changes Over Time") +
  labs(x = "Time", y = "Component", color = "Event Type") +
  theme(legend.position = "top")



