## 临时内容
### TODO
- [ ] 敏感接口的权限校验
- [ ] 实现接口限流功能

## 获取模型 API
### 前言
本项目主要使用的是阿里云百炼平台提供的 AI 模型服务，您可以根据自己的实际需求选择其他平台的模型服务。

### 操作流程
1. 进入阿里云百炼平台的[秘钥管理控制台](https://bailian.console.aliyun.com/cn-beijing/?tab=model#/api-key)。
2. 点击`创建API Key`按钮创建模型的 API Key。
3. 访问[模型市场](https://bailian.console.aliyun.com/cn-beijing/?productCode=p_efm#/model-market)选择合适的模型，本项目的 `OPENAI_MODEL_NAME` 默认使用的是 `qwen3-max`。

> [!TIP]
> - 初次使用阿里云百炼平台一般会获得三个月的免费额度，每天请求次数超限或者新用户免费体验时间到期都会产生收费，当阿里云钱包扣减到0时，无法调用 AI 接口。
> - 本项目默认对普通用户调用任意 AI 接口的速率限制为60次/小时，对管理员用户调用任意 AI 接口的速率限制为120次/小时，禁止游客调用 AI 接口。