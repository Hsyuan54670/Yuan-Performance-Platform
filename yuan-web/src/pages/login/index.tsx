import { LockOutlined, UserOutlined } from "@ant-design/icons";
import { Button, Card, Form, Input, Typography, message } from "antd";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { loginApi } from "../../api/auth";
import { useAuth } from "../../hooks/useAuth";
import type { LoginRequest } from "../../types/auth";
import { getRequestErrorMessage } from "../../utils/request";

function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const { t } = useTranslation();

  const onFinish = async (values: LoginRequest) => {
    try {
      const result = await loginApi(values);
      login(result.token, result.refreshToken, result.user);
      message.success(t("login.success"));
      navigate("/dashboard");
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("login.error")));
    }
  };

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "grid",
        placeItems: "center",
        background:
          "linear-gradient(140deg, rgba(255,232,204,0.95), rgba(240,249,255,0.95)), radial-gradient(circle at 0% 0%, #ff922b22, transparent 30%), radial-gradient(circle at 100% 100%, #0b728533, transparent 34%)"
      }}
    >
      <Card
        className="glass-card"
        style={{ width: 420, borderRadius: 22 }}
        styles={{ body: { padding: 28 } }}
      >
        <Typography.Title level={3} style={{ marginTop: 0, marginBottom: 4 }}>
          {t("app.title")}
        </Typography.Title>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 22 }}>
          {t("login.subtitle")}
        </Typography.Paragraph>

        <Form<LoginRequest> layout="vertical" onFinish={onFinish} initialValues={{ username: "admin", password: "admin123" }}>
          <Form.Item name="username" label={t("login.username")} rules={[{ required: true }]}>
            <Input size="large" prefix={<UserOutlined />} placeholder="admin" />
          </Form.Item>
          <Form.Item name="password" label={t("login.password")} rules={[{ required: true }]}>
            <Input.Password size="large" prefix={<LockOutlined />} placeholder="******" />
          </Form.Item>
          <Button type="primary" htmlType="submit" size="large" block>
            {t("login.signIn")}
          </Button>
        </Form>
      </Card>
    </div>
  );
}

export default LoginPage;
