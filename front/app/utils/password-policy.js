const MIN_PASSWORD_LENGTH = 8;
const MAX_PASSWORD_LENGTH = 72;

const letterPattern = /[A-Za-z]/;
const digitPattern = /\d/;
const specialPattern = /[^A-Za-z0-9]/;

export function getPasswordPolicyHint() {
  return `Пароль должен быть от ${MIN_PASSWORD_LENGTH} до ${MAX_PASSWORD_LENGTH} символов и содержать букву, цифру и спецсимвол.`;
}

export function validatePasswordPolicy(password) {
  const value = String(password || "");

  if (value.length < MIN_PASSWORD_LENGTH || value.length > MAX_PASSWORD_LENGTH) {
    return {
      valid: false,
      message: `Пароль должен быть от ${MIN_PASSWORD_LENGTH} до ${MAX_PASSWORD_LENGTH} символов.`,
    };
  }

  if (!letterPattern.test(value)) {
    return {
      valid: false,
      message: "Пароль должен содержать хотя бы одну букву.",
    };
  }

  if (!digitPattern.test(value)) {
    return {
      valid: false,
      message: "Пароль должен содержать хотя бы одну цифру.",
    };
  }

  if (!specialPattern.test(value)) {
    return {
      valid: false,
      message: "Пароль должен содержать хотя бы один спецсимвол.",
    };
  }

  return { valid: true, message: "" };
}
