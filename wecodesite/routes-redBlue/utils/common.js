export const getCommonConfig = async (lookupKey) => {
    return getLookupName(lookupKey);
}

const getLookupName = (lookupKey) => {
    if (lookupKey === 'CEC.Open/Flow.AppId.Config') {
        return demo
    } else {
        return demo2
    }
}

const demo = {
    code: 200,
    message: 'success',
    data: {
        lookups: {
            'CEC.Open/Flow.AppId.Config': {
                'classifyCode': 'Flow.AppId.Config',
                'classifyId': '123',
                'items': [
                    {
                        'itemCode': 'flow_max_qps',
                        'itemName': 'flow_max_qps',
                        'itemValue': "2000"
                    },
                    {
                        'itemCode': 'flow_max_serial_connector_nodes',
                        'itemName': 'flow_max_serial_connector_nodes',
                        'itemValue': "5"
                    },
                    {
                        'itemCode': 'flow_max_parrllel_branches',
                        'itemName': 'flow_max_parrllel_branches',
                        'itemValue': "5"
                    },
                    {
                        'itemCode': 'node_max_timeout_seconds',
                        'itemName': 'node_max_timeout_seconds',
                        'itemValue': "500000"
                    }
                ]
            }
        }
    }
}

const demo2 = {
    code: 200,
    message: 'success',
    data: {
        lookups: {
            'CEC.Open/Flow.AppId.19.Config': {
                'classifyCode': 'Flow.AppId.19.Config',
                'classifyId': '123',
                'items': [
                    // {
                    //     'itemCode': 'flow_max_qps',
                    //     'itemName': 'flow_max_qps',
                    //     'itemValue': "1500"
                    // },
                    {
                        'itemCode': 'flow_max_serial_connector_nodes',
                        'itemName': 'flow_max_serial_connector_nodes',
                        'itemValue': "5"
                    },
                    {
                        'itemCode': 'flow_max_parrllel_branches',
                        'itemName': 'flow_max_parrllel_branches',
                        'itemValue': "5"
                    },
                    {
                        'itemCode': 'node_max_timeout_seconds',
                        'itemName': 'node_max_timeout_seconds',
                        'itemValue': "500000"
                    }
                ]
            }
        }
    }
}