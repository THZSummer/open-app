export const getCommonConfig = async (lookupKey) => {
    return demo;
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
                        'itemCode': '19',
                        'itemName': '19',
                        'itemValue': "{\"QPS\":1500,\"showMoreFlowType\":true,\"serial\":5,\"parallel\":5,\"timeoutMs\":500000}"
                    }
                ]
            }
        }
    }
}