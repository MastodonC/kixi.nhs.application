(ns kixi.nhs.data.schema)

(def board-report-schema
  {:fields [{"id" "indicator_id"         "type" "text"}
            {"id" "value"                "type" "text"}
            {"id" "date"                 "type" "text"}
            {"id" "period_of_coverage"   "type" "text"}
            {"id" "start_date"           "type" "text"}
            {"id" "end_date"             "type" "text"}
            {"id" "uci"                  "type" "text"}
            {"id" "lci"                  "type" "text"}
            {"id" "parent_lens_fk"       "type" "text"}
            {"id" "lens_title"           "type" "text"}
            {"id" "lens_value"           "type" "text"}
            {"id" "lens_grouping"        "type" "text"}
            {"id" "sub_lens_resource_id" "type" "text"}]
   :primary-key "indicator_id,date,lens_value,period_of_coverage"})
