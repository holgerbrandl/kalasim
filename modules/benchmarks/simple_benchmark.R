#' ---
#' title: "Kalasim Performance Regressionn"
#' format:
#'   html:
#'     page-layout: full
#'     toc: true
#'     toc-depth: 3
#'     toc-title: "Contents"
#'     toc-location: left
#'     self-contained: true
#' lightbox: true
#' fig.width: 12
#' max-width: 90%
#' engine: knitr
#' knitr:
#'     opts_chunk:
#'       fig.width: 12
#'       fig.height: 7
#'       out.width: "100%"
#'       fig.align: "center"
#' ---

#' Load Packages

pacman::p_load(tidyverse, scales, grid, magrittr, readxl, digest, snakecase, lubridate)

pacman::p_load_gh("holgerbrandl/datautils")

scoreMetrics = list.files("simple_perf_logs", "simple.*.csv", full.names = TRUE) %>%
  map(~{
    read_csv(.x, show_col_types = FALSE) %>%
      mutate(
        ticktime = str_split_fixed(basename(.x), "[_]", 3)[, 2] %>% ymd,
        build = str_split_fixed(basename(.x), "[._]", 4)[, 3]%>%trim_ext
      )
  }) %>%
  bind_rows %>%
  # filter(str_detect(build, "lc-")) %>%
  pretty_columns


table_browser(scoreMetrics)

#+ fig.height=12
scoreMetrics %>%
  # filter(str_detect(build, "nu-")) %>%
  ggplot(aes(reorder(build, timestamp), runtime)) +
  geom_boxplot() +
  geom_jitter(alpha = 0.7) +
  ggtitle("Performance Progression") +
  scale_y_continuous(limits = c(0, NA)) + facet_grid(scenario~., scales="free_y")

