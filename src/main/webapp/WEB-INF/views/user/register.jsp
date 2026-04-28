<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>用户注册 - 校园二手交易平台</title>
    <link href="https://cdn.bootcdn.net/ajax/libs/bootstrap/4.6.2/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/static/css/main.css" rel="stylesheet">
    <style>
        .error-msg {
            color: #dc3545;
            font-size: 14px;
            margin-top: 10px;
            display: none;
        }
        .password-toggle {
            position: absolute;
            right: 10px;
            top: 50%;
            transform: translateY(-50%);
            cursor: pointer;
            color: #6c757d;
            user-select: none;
            z-index: 10;
        }
        .password-toggle:hover {
            color: #495057;
        }
        .password-wrapper {
            position: relative;
        }
        .password-wrapper input {
            padding-right: 40px;
        }
        .password-match {
            font-size: 12px;
            margin-top: 5px;
        }
        .match-success {
            color: #28a745;
        }
        .match-error {
            color: #dc3545;
        }
    </style>
</head>
<body style="background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); min-height: 100vh;">
    <div class="container" style="padding-top: 40px;">
        <div class="row justify-content-center">
            <div class="col-md-5 col-lg-4">
                <div class="text-center mb-4">
                    <h2 class="text-white">校园二手交易平台</h2>
                    <p class="text-white-50">注册账号，开始交易</p>
                </div>
                <div class="card card-ui" style="border-radius: 16px;">
                    <div class="card-body p-4">
                        <h4 class="text-center mb-2">用户注册</h4>
                        <p class="text-center text-muted mb-4">注册后可发布闲置、下单购买</p>
                        
                        <div id="errorMsg" class="alert alert-danger error-msg"></div>
                        
                        <form id="registerForm">
                            <div class="form-group">
                                <label><i class="text-danger">*</i> 用户名</label>
                                <input type="text" name="username" id="username" class="form-control" 
                                       placeholder="请输入用户名（登录时使用，不可重复）" required>
                                <small class="form-text text-muted">用户名用于登录，需唯一</small>
                            </div>
                            
                            <div class="form-group">
                                <label><i class="text-danger">*</i> 密码</label>
                                <div class="password-wrapper">
                                    <input type="password" name="password" id="password" class="form-control" 
                                           placeholder="请输入密码（至少6位）" required>
                                    <span class="password-toggle" onclick="togglePassword('password', this)">👁️</span>
                                </div>
                            </div>
                            
                            <div class="form-group">
                                <label><i class="text-danger">*</i> 确认密码</label>
                                <div class="password-wrapper">
                                    <input type="password" id="confirmPassword" class="form-control" 
                                           placeholder="请再次输入密码" required>
                                    <span class="password-toggle" onclick="togglePassword('confirmPassword', this)">👁️</span>
                                </div>
                                <div id="passwordMatchHint" class="password-match"></div>
                            </div>
                            
                            <div class="form-group">
                                <label>昵称</label>
                                <input type="text" name="nickname" class="form-control" 
                                       placeholder="可选，不填则使用用户名">
                                <small class="form-text text-muted">展示给其他用户的名称，可与他人重复</small>
                            </div>
                            
                            <div class="form-group">
                                <label>手机号</label>
                                <input type="text" name="phone" class="form-control" 
                                       placeholder="可选，方便买家联系">
                            </div>
                            
                            <div class="form-group">
                                <label>邮箱</label>
                                <input type="email" name="email" class="form-control" 
                                       placeholder="可选，用于接收通知">
                            </div>
                            
                            <button type="submit" class="btn btn-primary btn-block btn-lg mt-4">注 册</button>
                        </form>
                        
                        <hr class="my-4">
                        
                        <div class="text-center">
                            <span class="text-muted">已有账号？</span>
                            <a href="${ctx}/user/loginPage" class="text-primary">立即登录</a>
                        </div>
                        <div class="text-center mt-2">
                            <a href="${ctx}/" class="text-muted">← 返回首页</a>
                        </div>
                    </div>
                </div>
                
                <div class="text-center mt-4 text-white-50">
                    <small>© 2025 校园二手交易平台</small>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.bootcdn.net/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
    <script src="https://cdn.bootcdn.net/ajax/libs/bootstrap/4.6.2/js/bootstrap.bundle.min.js"></script>
    <script>
        // 切换密码显示/隐藏
        function togglePassword(inputId, toggleBtn) {
            var input = document.getElementById(inputId);
            if (input.type === 'password') {
                input.type = 'text';
                toggleBtn.textContent = '🙈';
            } else {
                input.type = 'password';
                toggleBtn.textContent = '👁️';
            }
        }

        // 检查两次密码是否一致
        function checkPasswordMatch() {
            var password = $('#password').val();
            var confirmPassword = $('#confirmPassword').val();
            var hint = $('#passwordMatchHint');
            
            if (confirmPassword === '') {
                hint.text('').removeClass('match-success match-error');
                return false;
            }
            
            if (password === confirmPassword) {
                hint.text('✓ 密码一致').removeClass('match-error').addClass('match-success');
                return true;
            } else {
                hint.text('✗ 密码不一致').removeClass('match-success').addClass('match-error');
                return false;
            }
        }

        // 监听密码输入
        $('#password, #confirmPassword').on('input', function() {
            checkPasswordMatch();
        });

        function showError(msg) {
            $('#errorMsg').text(msg).fadeIn();
        }

        function hideError() {
            $('#errorMsg').fadeOut();
        }

        $('#registerForm').submit(function(e) {
            e.preventDefault();
            hideError();
            
            var username = $('#username').val().trim();
            var password = $('#password').val();
            var confirmPassword = $('#confirmPassword').val();
            
            // 校验用户名
            if (!username) {
                showError('用户名不能为空');
                return;
            }
            
            if (username.length < 3) {
                showError('用户名至少3个字符');
                return;
            }
            
            // 校验密码
            if (!password) {
                showError('密码不能为空');
                return;
            }
            
            if (password.length < 6) {
                showError('密码至少6个字符');
                return;
            }
            
            // 校验确认密码
            if (!confirmPassword) {
                showError('请确认密码');
                return;
            }
            
            if (password !== confirmPassword) {
                showError('两次输入的密码不一致');
                return;
            }
            
            $.ajax({
                url: '${ctx}/user/register',
                type: 'POST',
                data: $(this).serialize(),
                success: function(result) {
                    if (result.success) {
                        alert('🎉 注册成功！请登录');
                        window.location.href = '${ctx}/user/loginPage';
                    } else {
                        showError(result.message || '注册失败');
                    }
                },
                error: function() {
                    showError('网络异常，请稍后重试');
                }
            });
        });
    </script>
</body>
</html>
