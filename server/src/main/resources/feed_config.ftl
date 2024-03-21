{
  "configs": [{
    "action": "ActivityFeed",
    "query": {
      "select": [{
          "operation": "shift",
          "spec": {
            "account_id": [
              "payload[0].account_id",
              "account_id"
            ],
            "payload": {
              "action_epoch": [
                "payload[0].timestamp",
                "timestamp"
              ],
              "actor": {
                "id": [
                  "payload[0].actor.id"
                ],
                "name": "payload[0].actor.name",
                "type": "payload[0].actor.type"
              },
              "event_info": "payload[0].content.event_info",
              "model_properties": {
                <#list items as item>
                  "${item.name}" : "payload[0].content.properties.&"<#sep>,
                </#list>
              },
              "changes": {
                "model_changes": "payload[0].content.changes"
              },
              "associations": "payload[0].content.associations"
            },
            "payload_type": "payload[0].action"
          }
        },
        {
          "operation": "modify-overwrite-beta",
          "spec": {
            "payload": {
              "*": {
                "timestamp": "=divide(@(1,timestamp), 1000)"
              }
            },
            "feedType": "alert",
            "links": [],
            "filters": {}
          }
        }
      ],
      "where": {
        "and": [
          {
            "key": "$payload_version",
            "regex": "1.0.*"
          },
          {
            "not": {
              "empty": "$payload.changes.model_changes"
            }
          }
        ]
      }
    }
  }]
}