package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.common.BusinessException;
import com.community.platform.common.ResultCode;
import com.community.platform.common.utils.JwtUtil;
import com.community.platform.dto.auth.LoginDTO;
import com.community.platform.dto.auth.ProfileUpdateDTO;
import com.community.platform.dto.auth.RegisterDTO;
import com.community.platform.dto.auth.ResetPasswordDTO;
import com.community.platform.entity.ResidentProfile;
import com.community.platform.entity.User;
import com.community.platform.entity.UserRole;
import com.community.platform.entity.WorkOrderLog;
import com.community.platform.entity.WorkOrder;
import com.community.platform.entity.ServiceBooking;
import com.community.platform.entity.Notice;
import com.community.platform.entity.ProxyRelation;
import com.community.platform.entity.ApplicationMaterial;
import com.community.platform.entity.ApplicationForm;
import com.community.platform.mapper.ResidentProfileMapper;
import com.community.platform.mapper.UserMapper;
import com.community.platform.mapper.UserRoleMapper;
import com.community.platform.mapper.ProxyRelationMapper;
import com.community.platform.mapper.ApplicationFormMapper;
import com.community.platform.mapper.ApplicationMaterialMapper;
import com.community.platform.mapper.ServiceBookingMapper;
import com.community.platform.mapper.NoticeMapper;
import com.community.platform.mapper.WorkOrderMapper;
import com.community.platform.mapper.WorkOrderLogMapper;
import com.community.platform.service.AuthService;
import com.community.platform.vo.auth.LoginVO;
import com.community.platform.vo.user.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Long BUILT_IN_ADMIN_ID = 0L;
    private static final String BUILT_IN_ADMIN_USERNAME = "admin";
    private static final String BUILT_IN_ADMIN_PASSWORD = "123456";

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final ResidentProfileMapper residentProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ProxyRelationMapper proxyRelationMapper;
    private final ApplicationFormMapper applicationFormMapper;
    private final ApplicationMaterialMapper applicationMaterialMapper;
    private final ServiceBookingMapper serviceBookingMapper;
    private final NoticeMapper noticeMapper;
    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderLogMapper workOrderLogMapper;

    @Override
    public LoginVO login(LoginDTO dto) {
        if (BUILT_IN_ADMIN_USERNAME.equals(dto.getUsername())) {
            if (!BUILT_IN_ADMIN_PASSWORD.equals(dto.getPassword())) {
                throw new BusinessException(ResultCode.PASSWORD_ERROR);
            }
            List<String> roles = List.of("ROLE_ADMIN");
            String token = jwtUtil.generateToken(BUILT_IN_ADMIN_ID, BUILT_IN_ADMIN_USERNAME, roles);
            LoginVO vo = new LoginVO();
            vo.setToken(token);
            vo.setUserId(BUILT_IN_ADMIN_ID);
            vo.setUsername(BUILT_IN_ADMIN_USERNAME);
            vo.setRoles(roles);
            return vo;
        }

        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        // if not found by username, try phone
        if (user == null) {
            user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getUsername()));
        }
        // if still not found, try account
        if (user == null) {
            user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getAccount, dto.getUsername()));
        }
        if (user == null) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        if ("disabled".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        List<String> roles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getUserId()))
                .stream().map(UserRole::getRoleCode).toList();

        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), roles);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRoles(roles);
        vo.setStaffType(user.getStaffType());
        vo.setBookingServiceType(user.getBookingServiceType());
        vo.setAvatar(user.getAvatar());
        return vo;
    }

    @Override
    public LoginVO loginBySms(String phone) {
        // For now SMS login simply finds user by phone and issues token.
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) throw new BusinessException(ResultCode.ACCOUNT_NOT_EXISTS);
        if ("disabled".equals(user.getStatus())) throw new BusinessException(ResultCode.ACCOUNT_DISABLED);

        List<String> roles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getUserId()))
                .stream().map(UserRole::getRoleCode).toList();

        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), roles);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRoles(roles);
        vo.setStaffType(user.getStaffType());
        vo.setBookingServiceType(user.getBookingServiceType());
        vo.setAvatar(user.getAvatar());
        return vo;
    }

    @Override
    @Transactional
    public void register(RegisterDTO dto) {
        boolean exists = userMapper.exists(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exists) {
            throw new BusinessException(ResultCode.ACCOUNT_EXISTS, "用户名已存在");
        }

        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            boolean phoneExists = userMapper.exists(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
            if (phoneExists) throw new BusinessException(ResultCode.ACCOUNT_EXISTS, "手机号已被注册");
        }

        if (dto.getAccount() != null && !dto.getAccount().isBlank()) {
            boolean accountExists = userMapper.exists(
                    new LambdaQueryWrapper<User>().eq(User::getAccount, dto.getAccount()));
            if (accountExists) throw new BusinessException(ResultCode.ACCOUNT_EXISTS, "系统账号已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        // set simple role on user record for compatibility with legacy checks
        if (dto.getAccount() != null) {
            user.setAccount(dto.getAccount());
        }
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setStatus("active");
        String requestedRole = dto.getRole() == null || dto.getRole().isBlank()
                ? "resident"
                : dto.getRole().trim().toLowerCase();
        if (!"resident".equals(requestedRole) && !"family".equals(requestedRole)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "公开注册仅支持居民用户或家属用户");
        }
        String userRoleSimple = requestedRole;
        userMapper.insert(user);

        UserRole role = new UserRole();
        role.setUserId(user.getUserId());
        String roleCode = "family".equals(requestedRole) ? "ROLE_FAMILY" : "ROLE_RESIDENT";
        role.setRoleCode(roleCode);
        userRoleMapper.insert(role);

        // persist simple role into user table (best-effort)
        user.setRole(userRoleSimple);
        userMapper.updateById(user);

        // check id card uniqueness
        if (dto.getIdCard() != null && !dto.getIdCard().isBlank()) {
            ResidentProfile existProfile = residentProfileMapper.selectOne(
                new LambdaQueryWrapper<ResidentProfile>().eq(ResidentProfile::getIdCard, dto.getIdCard()));
            if (existProfile != null) throw new BusinessException(ResultCode.ACCOUNT_EXISTS, "身份证号已被注册");
        }

        ResidentProfile profile = new ResidentProfile();
        profile.setUserId(user.getUserId());
        profile.setRealName(dto.getRealName());
        profile.setIdCard(dto.getIdCard());
        residentProfileMapper.insert(profile);
    }

    @Override
    public void logout(Long userId) {
        // Token 无状态，前端丢弃即可；如需黑名单可在此加入 Redis
        log.info("User {} logged out", userId);
    }

    @Override
    public UserInfoVO getMe(Long userId) {
        if (BUILT_IN_ADMIN_ID.equals(userId)) {
            UserInfoVO vo = new UserInfoVO();
            vo.setUserId(BUILT_IN_ADMIN_ID);
            vo.setUsername(BUILT_IN_ADMIN_USERNAME);
            vo.setRealName("系统管理员");
            vo.setRoles(List.of("ROLE_ADMIN"));
            return vo;
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_EXISTS);
        }

        List<String> roles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleCode).toList();

        ResidentProfile profile = residentProfileMapper.selectOne(
                new LambdaQueryWrapper<ResidentProfile>().eq(ResidentProfile::getUserId, userId));

        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setRoles(roles);
        vo.setStaffType(user.getStaffType());
        vo.setBookingServiceType(user.getBookingServiceType());
        if (profile != null) {
            vo.setRealName(profile.getRealName());
            vo.setIdCard(profile.getIdCard());
            vo.setAddress(profile.getAddress());
            vo.setAge(profile.getAge());
            vo.setGender(profile.getGender());
            vo.setBirthday(profile.getBirthday());
        }
        vo.setAvatar(user.getAvatar());
        return vo;
    }

    @Override
    @Transactional
    public void updateMe(Long userId, ProfileUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_EXISTS);
        }
        user.setEmail(dto.getEmail());
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        userMapper.updateById(user);

        ResidentProfile profile = residentProfileMapper.selectOne(
                new LambdaQueryWrapper<ResidentProfile>().eq(ResidentProfile::getUserId, userId));
        if (profile == null) {
            profile = new ResidentProfile();
            profile.setUserId(userId);
            profile.setRealName(dto.getRealName() == null || dto.getRealName().isBlank() ? user.getUsername() : dto.getRealName());
            profile.setResidentType("local");
            profile.setAddress(dto.getAddress());
            profile.setGender(dto.getGender());
            profile.setBirthday(dto.getBirthday());
            residentProfileMapper.insert(profile);
            return;
        }
        profile.setRealName(dto.getRealName());
        profile.setAddress(dto.getAddress());
        profile.setGender(dto.getGender());
        profile.setBirthday(dto.getBirthday());
        residentProfileMapper.updateById(profile);
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件为空");
        }
        String original = file.getOriginalFilename() == null ? "avatar" : file.getOriginalFilename();
        String ext = "png";
        int idx = original.lastIndexOf('.');
        if (idx > 0) ext = original.substring(idx + 1).toLowerCase();
        String stored = UUID.randomUUID().toString() + "." + ext;
        Path root = Paths.get("uploads/avatars").toAbsolutePath().normalize();
        Path target = root.resolve(stored);
        try {
            Files.createDirectories(root);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件保存失败");
        }

        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.ACCOUNT_NOT_EXISTS);
        String relative = "uploads/avatars/" + stored;
        user.setAvatar(relative);
        userMapper.updateById(user);
        return relative;
    }

    @Override
    public Resource loadAvatar(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getAvatar() == null) {
            return null;
        }
        Path path = Paths.get(user.getAvatar()).toAbsolutePath().normalize();
        try {
            Resource resource = new UrlResource(path.toUri());
            return resource.exists() && resource.isReadable() ? resource : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.ACCOUNT_NOT_EXISTS);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        log.info("User {} changed password", userId);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        // delete work order logs and work orders related to user's applications or assigned work
        workOrderLogMapper.delete(new LambdaQueryWrapper<WorkOrderLog>()
            .eq(WorkOrderLog::getOperatorId, userId));
        workOrderMapper.delete(new LambdaQueryWrapper<WorkOrder>()
            .eq(WorkOrder::getStaffUserId, userId));

        // delete application materials and forms where user is owner or proxy
        List<ApplicationForm> apps = applicationFormMapper.selectList(
            new LambdaQueryWrapper<ApplicationForm>()
                .eq(ApplicationForm::getUserId, userId)
                .or()
                .eq(ApplicationForm::getProxyUserId, userId)
        );
        if (apps != null && !apps.isEmpty()) {
            List<Long> appIds = apps.stream().map(ApplicationForm::getApplicationId).toList();
            applicationMaterialMapper.delete(new LambdaQueryWrapper<ApplicationMaterial>()
                .in(ApplicationMaterial::getApplicationId, appIds));
            applicationFormMapper.delete(new LambdaQueryWrapper<ApplicationForm>()
                .in(ApplicationForm::getApplicationId, appIds));
        }

        // delete bookings
        serviceBookingMapper.delete(new LambdaQueryWrapper<ServiceBooking>()
            .eq(ServiceBooking::getUserId, userId)
            .or()
            .eq(ServiceBooking::getProxyUserId, userId));

        // delete notices
        noticeMapper.delete(new LambdaQueryWrapper<Notice>()
            .eq(Notice::getUserId, userId));

        // delete proxy relations where user is proxy or target
        proxyRelationMapper.delete(new LambdaQueryWrapper<ProxyRelation>()
            .eq(ProxyRelation::getProxyUserId, userId)
            .or()
            .eq(ProxyRelation::getTargetUserId, userId));

        // delete resident profile
        residentProfileMapper.delete(new LambdaQueryWrapper<ResidentProfile>()
            .eq(ResidentProfile::getUserId, userId));

        // delete user roles
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
            .eq(UserRole::getUserId, userId));

        // finally delete user
        userMapper.deleteById(userId);
        log.info("Deleted user {} and related data", userId);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordDTO dto) {
        // 两次密码一致性校验
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "两次输入的密码不一致");
        }
        // 通过手机号查找用户
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_EXISTS, "该手机号未注册");
        }
        if ("disabled".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        // 更新密码（bcrypt加密，与changePassword保持一致）
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        log.info("User {} reset password via phone {}", user.getUserId(), dto.getPhone());
    }
}
