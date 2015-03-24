kixi.nhs.application
====================

Dev setup:

1. In your home directory create a `.nhs.edn` with the contents below:
   ```edn
   {:ckan-client {:site "http://<site>/api/3/action/"
                  :api-key "<your_private_key>"}
    :schedule {:process-job-schedule
               ; s   m  h  d  M D
               {"0  36 11  *  * ?" {:dest :board-report :type :update :resource-id "68ebcbee-177f-42b5-a31e-8f706d4ebf50"}}}
   }
   ```

2. Do `M-x cider-jack-in` in kixi.nhs.application project
3. Run `(go)`
4. Open up `dev/dev.clj` and try out some of the function there, e.g.
   `(dev/list-all-datasets system)` will print a list of *all*
   datasets in this client's CKAN

5. Try out some other functions:

  - `(take 10 (kixi.nhs.data.storage/get-resource-data (:ckan-client
    system) "68ebcbee-177f-42b5-a31e-8f706d4ebf50"))` to list 10
    values from that resource.

6. To create a new board report resource:

   ```clojure
   (use 'kixi.nhs.board-report)
   (insert-board-report-resource (:ckan-client system) "resources/prod_config.edn" "board_report")
   ```

7. To update an existing board report resource:

   ```clojure
   (update-board-report-dataset (:ckan-client system) "68ebcbee-177f-42b5-a31e-8f706d4ebf50" "resources/prod_config.edn")
   ```
