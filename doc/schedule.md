# Scheduler

The application uses Quartz Job Scheduler to schedule jobs,
e.g. updating of the board report.
Because the application is written in Clojure, a Clojure wrapper
Quartzite is used.
Schedule is an edn map, and is lcoated in `resources/default.nhs.edn`
file.
When the application is started, scheduler is started automatically
with it. Any jobs that are listed either in
`resources/default.nhs.edn` or in `~\.nhs.edn` files will be attempted
to run.


```
{
 :schedule {:process-job-schedule
            ;; s   m  h  d  M D
            { "0  30 10  *  * ?" {:dest :board-report :type :update :resource-id "68ebcbee-177f-42b5-a31e-8f706d4ebf50"}}}}
```

The key-value pairs contain the time at which a job is to be
triggered, and the details of that job, e.g. the job above is going to
be triggered at 10:30 am every day, its destination is `:board-report`
and type is `update`, and the resource-id that is going to be udapted
is `68ebcbee-177f-42b5-a31e-8f706d4ebf50`. Changing the time of the
trigger is very easy, e.g.
"0 45 10 * * ?" => every day at 10:45am
"0 0  12 2 * ?" => second day of each month at 12:00 pm
"0 0  12 ? * 7" => every Sunday of every month at 12:00 pm

For more information about the scheduling, please visit: http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger
