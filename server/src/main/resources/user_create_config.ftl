{
    "configs": [
      {
        "action": "Audit",
        "query": {
          "select": [
            {
              "operation": "shift",
              "spec": {
                "#user": [
                  "filters.type[0]",
                  "payload[0].content.type",
                  "payload[0].object.type"
                ],
                "payload_type": "filters.action[1]",
                "account_id": [
                  "account_id",
                  "payload[0].account_id"
                ],
                "payload": {
                  "action": [
                    "payload[0].action",
                    "filters.action[0]"
                  ],
                  "action_epoch": [
                    "timestamp",
                    "payload[0].timestamp"
                  ],
                  "actor": {
                    "id": [
                      "payload[0].actor.id",
                      "filters.actor[0]"
                    ],
                    "name": [
                      "payload[0].actor.name",
                      "payload[0].content.properties.actor.name"
                    ],
                    "type": "payload[0].actor.type"
                  },
                  "changes": {
                    "model_changes": "payload[0].content.changes",
                    "system_changes": "payload[0].content.system_changes"
                  },
                  "model_properties": {
                  <#list items as item>
                      "${item.name}" : "payload[0].content.properties.&"<#sep>,
                  </#list>
                  },
                  "event_info": {
                    "ip_address": "payload[0].content.properties.ip_address"
                  },
                  "associations": "payload[0].content.associations"
                }
              }
            },
            {
              "operation": "modify-overwrite-beta",
              "spec": {
                "filters": {
                  "*": {
                    "*": "=toString"
                  }
                },
                "payload": {
                  "*": {
                    "timestamp": "=divide(@(1,timestamp), 1000)",
                    "actor": {
                      "id": "=toString",
                      "type": "=toString"
                    },
                    "object": {
                      "id": "=toString"
                    }
                  }
                }
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
                "key": "$payload.accept",
                "eq": true
              },
              {
                "key": "$payload.event_info.meta.hypertail_requester_accept",
                "eq": true
              },
              {
                "key": "$payload.model_properties.helpdesk_agent",
                "eq": false
              },
              {
                "not": {
                  "key": "$payload.model_properties.user_role",
                  "in": [
                    1
                  ]
                }
              }
            ]
          }
        }
      }
    ]
 }
