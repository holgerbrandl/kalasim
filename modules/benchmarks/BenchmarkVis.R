#' Kalasim Benchmark
#'
# cd /d/projects/scheduling/kalasim/modules/benchmarks; R
# rend.R -e BenchmarkVis.R

pacman::p_load(tidyverse, scales, grid, magrittr, readxl, digest, snakecase, lubridate)
pacman::p_load_gh("holgerbrandl/datautils")

# pacman::p_load_gh(jsonlite)


# https://stackoverflow.com/questions/23089895/how-to-remove-time-field-string-from-a-date-as-character-variable
parse_timestamp = function(x) {
  str_replace(x, "T", " ") |> parse_date_time("%Y%m%d %H%M%S")
}

# perfData = "perf_logs/benchmarks.csv" %>%
perfData = list.files("perf_logs", "^results_*", full = T) %>%
  map(~{
    read_csv(.x, show_col_types = FALSE) %>%
      mutate(
        run = basename(.x) %>%
        { str_split_fixed(., fixed("_"), 3)[, 2] } %>%
          parse_timestamp,
        commit = str_split_fixed(basename(.x), fixed("_"), 3)[, 3] %>% trim_ext(".csv")
      )
  }) %>%
  bind_rows %>%
  pretty_columns() %>%
  rename(ci = `score_error_99_9`)

perfData %<>% mutate(benchmark = map_chr(str_split(benchmark, "[.]"), last))

perfData %<>% filter_count(mode == "avgt")

perfData %>% distinct(unit)

#' merge parameter columns
perfData %<>% unite("params", starts_with("param"), na.rm = T, remove = F)


#' show the data
perfData %>% glimpse

table_browser(perfData, "Benchmark Data")


# count(perfData, benchmark)
#' show the data all in one big panel
perfData %>%
  ggplot(aes(run, score, color = params)) +
  geom_line() +
  geom_point() +
  # geom_col() +
  geom_errorbar(aes(run, ymin = score - ci, ymax = score + ci)) +
  facet_wrap(~benchmark, ncol = 1, scales = "free_y") +
  ggtitle("benchmark results")  #+ scale_x_log10()


## Note: by using json we would have better access to raw data
# jsonData = fromJSON("perf_logs/benchmarks.csv")
# results = flatten(jsonData) %>% transmute(benchmark, runtime = primaryMetric.score, ci = primaryMetric.scoreError)
# results$benchmark %<>% str_match("Benchmark.(.*)") %>% get_col(2)
#
# # note 1% CI is used here
# results %>% ggplot(aes(benchmark, runtime)) +
#     geom_bar(stat = "identity") +
#     geom_errorbar(aes(ymin = runtime - ci, ymax = runtime + ci), width = .2)

#' Also study individual plots
plotBench = function(aBM) {
  gg = perfData %>%
    filter(benchmark == aBM) %>%
    ggplot(aes(as.factor(run), score, color = commit)) +
    # geom_line() +
    geom_point() +
    # geom_col() +
    geom_errorbar(aes(ymin = score - ci, ymax = score + ci), width = .2) +
    rot_x_lab() +
    ggtitle(aBM) +
    facet_wrap(~params, ncol = 1) +
    guides(color = "none")

  print(gg)
}

# #+ fig.width=14, fig.height=14
unique(perfData$benchmark) %>% walk(plotBench)

#' Once again but with a continuous time axis
plotBench = function(aBM) {
  gg = perfData %>%
    filter(benchmark == aBM) %>%
    ggplot(aes(run, score, color = commit)) +
    # geom_line() +
    geom_point() +
    # geom_col() +
    geom_errorbar(aes(ymin = score - ci, ymax = score + ci), width = .2) +
    ggtitle(aBM)+
    facet_wrap(~params, ncol = 1) +
    guides(color = "none")

  print(gg)
}

# #+ fig.width=14, fig.height=14
unique(perfData$benchmark) %>% walk(plotBench)
