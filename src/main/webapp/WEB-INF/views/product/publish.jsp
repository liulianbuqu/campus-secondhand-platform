<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>发布商品</title>
    <link href="https://cdn.bootcdn.net/ajax/libs/bootstrap/4.6.2/css/bootstrap.min.css" rel="stylesheet">
    <link href="${ctx}/static/css/main.css" rel="stylesheet">
</head>
<body>
    <nav class="navbar navbar-expand-lg navbar-light bg-white">
        <div class="container">
            <a class="navbar-brand brand-strong" href="${ctx}/">校园二手交易平台</a>
        </div>
    </nav>

    <div class="container mt-4">
        <div class="row justify-content-center">
            <div class="col-md-8">
                <div class="card card-ui">
                    <div class="card-body">
                        <h4 class="mb-3">发布商品</h4>
                        <p class="text-muted mb-4">填写商品信息，上传清晰图片，可以大大提高成交率</p>
                        <form id="publishForm" enctype="multipart/form-data">
                            <div class="form-group">
                                <label>商品名称 *</label>
                                <input type="text" name="name" class="form-control" required>
                            </div>
                            <div class="form-group">
                                <label>价格 *</label>
                                <input type="number" name="price" class="form-control" step="0.01" min="0" required>
                            </div>
                            <div class="form-group">
                                <label>分类 *</label>
                                <select name="categoryId" class="form-control" required>
                                    <option value="">请选择分类</option>
                                    <c:forEach items="${categories}" var="category">
                                        <option value="${category.id}">${category.categoryName}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="form-group">
                                <label>商品图片 *</label>
                                <input type="file" name="imageFile" class="form-control" accept="image/*" required>
                                <small class="form-text text-muted">建议 1~3 张清晰图片，支持 jpg/png</small>
                            </div>
                            <div class="form-group">
                                <label>商品描述 *</label>
                                <textarea name="description" class="form-control" rows="5" required
                                          placeholder="例如：购买时间、成色、使用情况、是否包邮等"></textarea>
                            </div>
                            <button type="submit" class="btn btn-primary">发布</button>
                            <a href="${ctx}/product/manage" class="btn btn-secondary">取消</a>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.bootcdn.net/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
    <script src="https://cdn.bootcdn.net/ajax/libs/bootstrap/4.6.2/js/bootstrap.bundle.min.js"></script>
    <script>
        $('#publishForm').submit(function(e) {
            e.preventDefault();
            var formData = new FormData(this);
            $.ajax({
                url: '${ctx}/product/publish',
                type: 'POST',
                data: formData,
                processData: false,
                contentType: false,
                success: function(result) {
                    if (result.success) {
                        alert('发布成功！');
                        window.location.href = '${ctx}/product/manage';
                    } else {
                        alert(result.message || '发布失败');
                    }
                },
                error: function(xhr) {
                    var msg = '发布失败，请稍后重试';
                    try {
                        if (xhr.responseJSON && xhr.responseJSON.message) {
                            msg = xhr.responseJSON.message;
                        }
                    } catch (e) {
                        // ignore json parse error
                    }
                    alert(msg);
                    if (xhr.status === 401) {
                        window.location.href = '${ctx}/user/loginPage';
                    }
                }
            });
        });
    </script>
</body>
</html>



