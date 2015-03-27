# Recipes

There are a few files that store the details of the recipes. All of
them are located in `resources` directory.

## prod_config.edn

It lists information about the board report: publisher, name,
title. It also describes recipes for most of the indicators. Example
of a recipe:

```clojure
{:indicator-id "45"
 :fields-to-extract [:indicator_value :year :period_of_coverage]
 :fields-to-rename {:year :date
                    :indicator_value :value
                    :indicator_value_rate :value
                    :average_health_gain :value}
 :conditions [{:field :level_description :values #{"England"}}]
 :metadata {:sub_lens_resource_id "" :lens_value "England"}
 :calculate-ci true
 :format {:percentage :divide}
 :resource-id "800d0b08-95f7-4a0f-abd5-8bcb2528d8b4"}
```
Each recipe lists the indicator-id, resource-id of the CKAN resource
that is to be used to extract/calculate value(s), fields the are to be
renamed (e.g. `:year` is going to be renamed to `:date`, conditions
for filtering the data (e.g. all rows where `:level_description`
contains `England`, and a couple of flags: whether CI should be
calculated and whether the format of the numerical value should be
changed (if we expect a percentage, we want to unify to be in a form
of `XX.XX`, e.g. `0.91`).

## recipes/constitution.edn

All NHS Constitution recipes are located in a separate file (to allow
for clearer view).

Example:

```clojure

;; February 2014
{:resource-id "5be78264-23be-4e8f-8ba1-4c4b80aea12b"
 :indicator-id "126"
 :headers "resources/headers/nhs_constitution.edn"
 :worksheet {:tab "National" :headers ["National" :generic]}
 :scrub-details {:offset 14 :last-row 34}
 :metadata {:sub_lens_resource_id "" :lens_value "England"
            :date "01-02-2014" :period_of_coverage "2014 February"}
 :fields-to-extract [:treatment_function :within_18_weeks_percentile]
 :fields-to-rename {:within_18_weeks_percentile :value}
 :conditions [{:field :treatment_function :values #{"Total"}}]
 :format {:percentage :none}
 :field :within_18_weeks_percentile
 :operation :none}
```
Historical data is added by repeating the same recipe for a given
indicator, and updating the `resource-id` with a given date's resource
is CKAN and updating metadata with `date` and `period_of_coverage`.

## Headers

Some resources in CKAN were not imported to DataStore as they were
multisheet xls files and so they are being processed as such. In order
to extract the data from the xls file, we have to add appropriate
headers. These are located in `resources/headers` directory. There are
some variations between the number or the order of headers in
historical data, and hence most of the time there is a list of headers
per month. If new data source is added that is an xls file, please
provide appropriate headers in a similar way to the existing ones.
