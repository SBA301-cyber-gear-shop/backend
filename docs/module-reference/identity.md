# Tra cứu module identity

Tài liệu trích tự động từ source Java. Chữ ký và lời gọi thể hiện cú pháp tĩnh, không chứng minh thứ tự chạy hoặc đích thực thi. Xem source để biết validation, các nhánh và SQL. Getter/setter do Lombok sinh không được liệt kê.

## HTTP endpoint

Chữ ký bên dưới thể hiện tham số và kiểu trả về. Authentication do Spring cung cấp. Quyền có thể nằm trong security config, annotation hoặc thân hàm.

- **POST `/api/auth/register`** → `AuthController.register` [AuthController.java:29](../../src/main/java/com/project/cybershop/identity/internal/controller/AuthController.java#L29)
  - `@PostMapping("/register") @Operation(summary = "Đăng ký tài khoản khách hàng mới") public ResponseEntity<ApiResponseDTO<AuthResponse>> register(@Valid @RequestBody RegisterRequest request)`
- **POST `/api/auth/login`** → `AuthController.login` [AuthController.java:37](../../src/main/java/com/project/cybershop/identity/internal/controller/AuthController.java#L37)
  - `@PostMapping("/login") @Operation(summary = "Đăng nhập bằng Email/Số điện thoại và Mật khẩu") public ResponseEntity<ApiResponseDTO<AuthResponse>> login(@Valid @RequestBody LoginRequest request)`
- **GET `/api/auth/me`** → `AuthController.getCurrentUser` [AuthController.java:44](../../src/main/java/com/project/cybershop/identity/internal/controller/AuthController.java#L44)
  - `@GetMapping("/me") @Operation(summary = "Lấy thông tin tài khoản hiện tại qua Bearer Token") public ResponseEntity<ApiResponseDTO<UserDto>> getCurrentUser( @RequestHeader(value = "Authorization", required = false) String authHeader)`
- **PUT `/api/auth/me`** → `AuthController.updateProfile` [AuthController.java:52](../../src/main/java/com/project/cybershop/identity/internal/controller/AuthController.java#L52)
  - `@PutMapping("/me") @Operation(summary = "Cập nhật họ tên, số điện thoại và địa chỉ của tài khoản hiện tại") public ResponseEntity<ApiResponseDTO<UserDto>> updateProfile( @RequestHeader(value = "Authorization", required = false) String authHeader, @Valid @RequestBody UpdateProfileRequest request)`

## Khai báo và lời gọi theo file

## `identity/api/dto/request/LoginRequest.java`

### LoginRequest — [LoginRequest.java:9](../../src/main/java/com/project/cybershop/identity/api/dto/request/LoginRequest.java#L9)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class LoginRequest`

- Trường: `@NotBlank(message = "Email hoặc số điện thoại không được để trống") private String identity;`
- Trường: `@NotBlank(message = "Mật khẩu không được để trống") private String password;`

## `identity/api/dto/request/RegisterRequest.java`

### RegisterRequest — [RegisterRequest.java:12](../../src/main/java/com/project/cybershop/identity/api/dto/request/RegisterRequest.java#L12)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class RegisterRequest`

- Trường: `@NotBlank(message = "Họ và tên không được để trống") private String fullName;`
- Trường: `@NotBlank(message = "Email không được để trống") @Email(message = "Email không hợp lệ") private String email;`
- Trường: `@Pattern(regexp = "^(0[0-9]{9})?$", message = "Số điện thoại phải có 10 chữ số bắt đầu bằng 0") private String phone;`
- Trường: `@NotBlank(message = "Mật khẩu không được để trống") @Size(min = 6, message = "Mật khẩu phải có tối thiểu 6 ký tự") private String password;`
- Trường: `private String address;`

## `identity/api/dto/request/UpdateProfileRequest.java`

### UpdateProfileRequest — [UpdateProfileRequest.java:8](../../src/main/java/com/project/cybershop/identity/api/dto/request/UpdateProfileRequest.java#L8)

Khai báo: `@Data public class UpdateProfileRequest`

- Trường: `@NotBlank(message = "Họ và tên không được để trống") @Size(max = 100, message = "Họ và tên tối đa 100 ký tự") private String fullName;`
- Trường: `@NotBlank(message = "Số điện thoại không được để trống") @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải có 10 chữ số bắt đầu bằng 0") private String phone;`
- Trường: `@Size(max = 500, message = "Địa chỉ tối đa 500 ký tự") private String address;`

## `identity/api/dto/response/AuthResponse.java`

### AuthResponse — [AuthResponse.java:8](../../src/main/java/com/project/cybershop/identity/api/dto/response/AuthResponse.java#L8)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class AuthResponse`

- Trường: `private String accessToken;`
- Trường: `@Builder.Default private String tokenType = "Bearer";`
- Trường: `private UserDto user;`

## `identity/api/dto/response/UserDto.java`

### UserDto — [UserDto.java:10](../../src/main/java/com/project/cybershop/identity/api/dto/response/UserDto.java#L10)

Khai báo: `@Data @Builder @NoArgsConstructor @AllArgsConstructor public class UserDto`

- Trường: `private Integer id;`
- Trường: `private String fullName;`
- Trường: `private String email;`
- Trường: `private String phone;`
- Trường: `private String address;`
- Trường: `private String role;`
- Trường: `private Boolean isActive;`
- Trường: `private LocalDateTime createdAt;`

## `identity/api/IdentityApi.java`

### IdentityApi — [IdentityApi.java:7](../../src/main/java/com/project/cybershop/identity/api/IdentityApi.java#L7)

Khai báo: `public interface IdentityApi`

- [IdentityApi.java:8](../../src/main/java/com/project/cybershop/identity/api/IdentityApi.java#L8) `void lockActiveUser(Integer userId)`
- [IdentityApi.java:10](../../src/main/java/com/project/cybershop/identity/api/IdentityApi.java#L10) `default UserDto requireActiveUser(String email)`
  - Lời gọi xuất hiện: `getUserByEmail(email).filter(user -> Boolean.TRUE.equals(user.getIsActive())).orElseThrow()`, `getUserByEmail(email).filter()`, `getUserByEmail()`, `Boolean.TRUE.equals()`, `user.getIsActive()`
- [IdentityApi.java:15](../../src/main/java/com/project/cybershop/identity/api/IdentityApi.java#L15) `Optional<UserDto> getUserById(Integer id)`
- [IdentityApi.java:16](../../src/main/java/com/project/cybershop/identity/api/IdentityApi.java#L16) `Optional<UserDto> getUserByEmail(String email)`
- [IdentityApi.java:17](../../src/main/java/com/project/cybershop/identity/api/IdentityApi.java#L17) `boolean existsById(Integer id)`

## `identity/internal/controller/AuthController.java`

### AuthController — [AuthController.java:20](../../src/main/java/com/project/cybershop/identity/internal/controller/AuthController.java#L20)

Khai báo: `@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor @Tag(name = "Authentication", description = "API Xác thực người dùng (Đăng ký, Đăng nhập, Thông tin tài khoản)") public class AuthController`

- Trường: `private final AuthService authService;`
- Trường: `private final JwtUtil jwtUtil;`
- [AuthController.java:29](../../src/main/java/com/project/cybershop/identity/internal/controller/AuthController.java#L29) `@PostMapping("/register") @Operation(summary = "Đăng ký tài khoản khách hàng mới") public ResponseEntity<ApiResponseDTO<AuthResponse>> register(@Valid @RequestBody RegisterRequest request)`
  - Lời gọi xuất hiện: `authService.register()`, `ResponseEntity.status(HttpStatus.CREATED).body()`, `ResponseEntity.status()`, `ApiResponseDTO.success()`
- [AuthController.java:37](../../src/main/java/com/project/cybershop/identity/internal/controller/AuthController.java#L37) `@PostMapping("/login") @Operation(summary = "Đăng nhập bằng Email/Số điện thoại và Mật khẩu") public ResponseEntity<ApiResponseDTO<AuthResponse>> login(@Valid @RequestBody LoginRequest request)`
  - Lời gọi xuất hiện: `authService.login()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`
- [AuthController.java:44](../../src/main/java/com/project/cybershop/identity/internal/controller/AuthController.java#L44) `@GetMapping("/me") @Operation(summary = "Lấy thông tin tài khoản hiện tại qua Bearer Token") public ResponseEntity<ApiResponseDTO<UserDto>> getCurrentUser( @RequestHeader(value = "Authorization", required = false) String authHeader)`
  - Lời gọi xuất hiện: `authService.getCurrentUser()`, `emailFromBearer()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`
- [AuthController.java:52](../../src/main/java/com/project/cybershop/identity/internal/controller/AuthController.java#L52) `@PutMapping("/me") @Operation(summary = "Cập nhật họ tên, số điện thoại và địa chỉ của tài khoản hiện tại") public ResponseEntity<ApiResponseDTO<UserDto>> updateProfile( @RequestHeader(value = "Authorization", required = false) String authHeader, @Valid @RequestBody UpdateProfileRequest request)`
  - Lời gọi xuất hiện: `authService.updateProfile()`, `emailFromBearer()`, `ResponseEntity.ok()`, `ApiResponseDTO.success()`
- [AuthController.java:62](../../src/main/java/com/project/cybershop/identity/internal/controller/AuthController.java#L62) `private String emailFromBearer(String authHeader)`
  - Lời gọi xuất hiện: `authHeader.startsWith()`, `authHeader.substring()`, `jwtUtil.validateToken()`, `jwtUtil.getEmailFromToken()`

## `identity/internal/entity/Role.java`

### Role — [Role.java:8](../../src/main/java/com/project/cybershop/identity/internal/entity/Role.java#L8)

Khai báo: `@Entity @Table(name = "roles") @Getter @Setter @NoArgsConstructor public class Role`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@Column(name = "name", nullable = false, unique = true, length = 50) private String name;`
- Trường: `@Column(name = "description", length = 255) private String description;`
- [Role.java:25](../../src/main/java/com/project/cybershop/identity/internal/entity/Role.java#L25) `public Role(String name, String description)`

## `identity/internal/entity/User.java`

### User — [User.java:10](../../src/main/java/com/project/cybershop/identity/internal/entity/User.java#L10)

Khai báo: `@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor public class User`

- Trường: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Trường: `@ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "role_id", nullable = false) private Role role;`
- Trường: `@Column(name = "full_name", nullable = false, length = 100) private String fullName;`
- Trường: `@Column(name = "email", nullable = false, unique = true, length = 100) private String email;`
- Trường: `@Column(name = "phone", unique = true, length = 20) private String phone;`
- Trường: `@Column(name = "password_hash", nullable = false, length = 255) private String passwordHash;`
- Trường: `@Column(name = "address", columnDefinition = "TEXT") private String address;`
- Trường: `@Column(name = "is_active") private Boolean isActive = true;`
- Trường: `@Column(name = "created_at") private LocalDateTime createdAt;`
- Trường: `@Column(name = "updated_at") private LocalDateTime updatedAt;`
- [User.java:49](../../src/main/java/com/project/cybershop/identity/internal/entity/User.java#L49) `@PrePersist public void prePersist()`
  - Lời gọi xuất hiện: `LocalDateTime.now()`
- [User.java:56](../../src/main/java/com/project/cybershop/identity/internal/entity/User.java#L56) `@PreUpdate public void preUpdate()`
  - Lời gọi xuất hiện: `LocalDateTime.now()`
- [User.java:61](../../src/main/java/com/project/cybershop/identity/internal/entity/User.java#L61) `public User(Role role, String fullName, String email, String phone, String passwordHash, String address)`

## `identity/internal/mapper/UserMapper.java`

### UserMapper — [UserMapper.java:13](../../src/main/java/com/project/cybershop/identity/internal/mapper/UserMapper.java#L13)

Khai báo: `@Mapper(componentModel = "spring") public interface UserMapper`

- [UserMapper.java:16](../../src/main/java/com/project/cybershop/identity/internal/mapper/UserMapper.java#L16) `@Mapping(target = "role", source = "role", qualifiedByName = "formatRole") UserDto toDto(User user)`
- [UserMapper.java:19](../../src/main/java/com/project/cybershop/identity/internal/mapper/UserMapper.java#L19) `List<UserDto> toDtoList(List<User> users)`
- [UserMapper.java:21](../../src/main/java/com/project/cybershop/identity/internal/mapper/UserMapper.java#L21) `@Mapping(target = "id", ignore = true) @Mapping(target = "role", ignore = true) @Mapping(target = "passwordHash", ignore = true) @Mapping(target = "isActive", constant = "true") @Mapping(target = "createdAt", ignore = true) @Mapping(target = "updatedAt", ignore = true) User toEntity(RegisterRequest request)`
- [UserMapper.java:29](../../src/main/java/com/project/cybershop/identity/internal/mapper/UserMapper.java#L29) `@Named("formatRole") default String formatRole(Role role)`
  - Lời gọi xuất hiện: `role.getName()`, `role.getName().toUpperCase()`

## `identity/internal/repository/RoleRepository.java`

### RoleRepository — [RoleRepository.java:9](../../src/main/java/com/project/cybershop/identity/internal/repository/RoleRepository.java#L9)

Khai báo: `@Repository public interface RoleRepository extends JpaRepository<Role, Integer>`

- [RoleRepository.java:11](../../src/main/java/com/project/cybershop/identity/internal/repository/RoleRepository.java#L11) `Optional<Role> findByNameIgnoreCase(String name)`
- [RoleRepository.java:12](../../src/main/java/com/project/cybershop/identity/internal/repository/RoleRepository.java#L12) `boolean existsByNameIgnoreCase(String name)`

## `identity/internal/repository/UserRepository.java`

### UserRepository — [UserRepository.java:11](../../src/main/java/com/project/cybershop/identity/internal/repository/UserRepository.java#L11)

Khai báo: `@Repository public interface UserRepository extends JpaRepository<User, Integer>`

- [UserRepository.java:14](../../src/main/java/com/project/cybershop/identity/internal/repository/UserRepository.java#L14) `@Query(value = """ SELECT id FROM users WHERE id = :userId AND is_active = true FOR UPDATE """, nativeQuery = true) Optional<Integer> findActiveIdForUpdate(@Param("userId") Integer userId)`
- [UserRepository.java:21](../../src/main/java/com/project/cybershop/identity/internal/repository/UserRepository.java#L21) `Optional<User> findByEmailIgnoreCase(String email)`
- [UserRepository.java:22](../../src/main/java/com/project/cybershop/identity/internal/repository/UserRepository.java#L22) `Optional<User> findByPhone(String phone)`
- [UserRepository.java:23](../../src/main/java/com/project/cybershop/identity/internal/repository/UserRepository.java#L23) `boolean existsByEmailIgnoreCase(String email)`
- [UserRepository.java:24](../../src/main/java/com/project/cybershop/identity/internal/repository/UserRepository.java#L24) `boolean existsByPhone(String phone)`
- [UserRepository.java:26](../../src/main/java/com/project/cybershop/identity/internal/repository/UserRepository.java#L26) `@Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:identity) OR u.phone = :identity") Optional<User> findByIdentity(@Param("identity") String identity)`

## `identity/internal/service/AuthService.java`

### AuthService — [AuthService.java:9](../../src/main/java/com/project/cybershop/identity/internal/service/AuthService.java#L9)

Khai báo: `public interface AuthService`

- [AuthService.java:10](../../src/main/java/com/project/cybershop/identity/internal/service/AuthService.java#L10) `AuthResponse register(RegisterRequest request)`
- [AuthService.java:11](../../src/main/java/com/project/cybershop/identity/internal/service/AuthService.java#L11) `AuthResponse login(LoginRequest request)`
- [AuthService.java:12](../../src/main/java/com/project/cybershop/identity/internal/service/AuthService.java#L12) `UserDto getCurrentUser(String email)`
- [AuthService.java:13](../../src/main/java/com/project/cybershop/identity/internal/service/AuthService.java#L13) `UserDto updateProfile(String email, UpdateProfileRequest request)`

## `identity/internal/service/impl/AuthServiceImpl.java`

### AuthServiceImpl — [AuthServiceImpl.java:24](../../src/main/java/com/project/cybershop/identity/internal/service/impl/AuthServiceImpl.java#L24)

Khai báo: `@Service public class AuthServiceImpl implements AuthService`

- Trường: `private final UserRepository userRepository;`
- Trường: `private final RoleRepository roleRepository;`
- Trường: `private final PasswordEncoder passwordEncoder;`
- Trường: `private final JwtUtil jwtUtil;`
- Trường: `private final UserMapper userMapper;`
- Trường: `private final CartRepository cart;`
- [AuthServiceImpl.java:34](../../src/main/java/com/project/cybershop/identity/internal/service/impl/AuthServiceImpl.java#L34) `public AuthServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, UserMapper userMapper, CartRepository cart)`
- [AuthServiceImpl.java:47](../../src/main/java/com/project/cybershop/identity/internal/service/impl/AuthServiceImpl.java#L47) `@Override @Transactional public AuthResponse register(RegisterRequest request)`
  - Lời gọi xuất hiện: `request.getEmail().trim().toLowerCase()`, `request.getEmail().trim()`, `request.getEmail()`, `userRepository.existsByEmailIgnoreCase()`, `request.getPhone()`, `request.getPhone().isBlank()`, `request.getPhone().trim()`, `userRepository.existsByPhone()`, `roleRepository.findByNameIgnoreCase("CUSTOMER").orElseGet()`, `roleRepository.findByNameIgnoreCase()`, `roleRepository.save()`, `userMapper.toEntity()`, `user.setEmail()`, `user.setRole()`, `user.setPasswordHash()`, `passwordEncoder.encode()`, `request.getPassword()`, `user.setPhone()`, `request.getAddress()`, `request.getAddress().isBlank()`, `user.setAddress()`, `request.getAddress().trim()`, `userRepository.save()`, `newCart.setUserId()`, `user.getId()`, `newCart.setUpdatedAt()`, `java.time.LocalDateTime.now()`, `cart.save()`, `jwtUtil.generateToken()`, `user.getEmail()`, `user.getRole().getName()`, `user.getRole()`, `AuthResponse.builder() .accessToken(token) .tokenType("Bearer") .user(userMapper.toDto(user)).build()`, `AuthResponse.builder() .accessToken(token) .tokenType("Bearer").user()`, `AuthResponse.builder() .accessToken(token).tokenType()`, `AuthResponse.builder().accessToken()`, `AuthResponse.builder()`, `userMapper.toDto()`
- [AuthServiceImpl.java:95](../../src/main/java/com/project/cybershop/identity/internal/service/impl/AuthServiceImpl.java#L95) `@Override @Transactional(readOnly = true) public AuthResponse login(LoginRequest request)`
  - Lời gọi xuất hiện: `request.getIdentity().trim()`, `request.getIdentity()`, `userRepository.findByIdentity(identity).orElseThrow()`, `userRepository.findByIdentity()`, `passwordEncoder.matches()`, `request.getPassword()`, `user.getPasswordHash()`, `Boolean.TRUE.equals()`, `user.getIsActive()`, `jwtUtil.generateToken()`, `user.getEmail()`, `user.getRole().getName()`, `user.getRole()`, `AuthResponse.builder() .accessToken(token) .tokenType("Bearer") .user(userMapper.toDto(user)).build()`, `AuthResponse.builder() .accessToken(token) .tokenType("Bearer").user()`, `AuthResponse.builder() .accessToken(token).tokenType()`, `AuthResponse.builder().accessToken()`, `AuthResponse.builder()`, `userMapper.toDto()`
- [AuthServiceImpl.java:119](../../src/main/java/com/project/cybershop/identity/internal/service/impl/AuthServiceImpl.java#L119) `@Override @Transactional(readOnly = true) public UserDto getCurrentUser(String email)`
  - Lời gọi xuất hiện: `userRepository.findByEmailIgnoreCase(email).orElseThrow()`, `userRepository.findByEmailIgnoreCase()`, `userMapper.toDto()`
- [AuthServiceImpl.java:127](../../src/main/java/com/project/cybershop/identity/internal/service/impl/AuthServiceImpl.java#L127) `@Override @Transactional public UserDto updateProfile(String email, UpdateProfileRequest request)`
  - Lời gọi xuất hiện: `userRepository.findByEmailIgnoreCase(email).orElseThrow()`, `userRepository.findByEmailIgnoreCase()`, `Boolean.TRUE.equals()`, `user.getIsActive()`, `request.getPhone().trim()`, `request.getPhone()`, `phone.equals()`, `user.getPhone()`, `userRepository.existsByPhone()`, `user.setFullName()`, `request.getFullName().trim()`, `request.getFullName()`, `user.setPhone()`, `request.getAddress()`, `user.setAddress()`, `address.isBlank()`, `address.trim()`, `userMapper.toDto()`, `userRepository.save()`

## `identity/internal/service/impl/IdentityApiImpl.java`

### IdentityApiImpl — [IdentityApiImpl.java:14](../../src/main/java/com/project/cybershop/identity/internal/service/impl/IdentityApiImpl.java#L14)

Khai báo: `@Service @Transactional(readOnly = true) @RequiredArgsConstructor public class IdentityApiImpl implements IdentityApi`

- Trường: `private final UserRepository userRepository;`
- Trường: `private final UserMapper userMapper;`
- [IdentityApiImpl.java:22](../../src/main/java/com/project/cybershop/identity/internal/service/impl/IdentityApiImpl.java#L22) `@Override @Transactional public void lockActiveUser(Integer userId)`
  - Lời gọi xuất hiện: `userRepository.findActiveIdForUpdate(userId).isEmpty()`, `userRepository.findActiveIdForUpdate()`
- [IdentityApiImpl.java:30](../../src/main/java/com/project/cybershop/identity/internal/service/impl/IdentityApiImpl.java#L30) `@Override public Optional<UserDto> getUserById(Integer id)`
  - Lời gọi xuất hiện: `userRepository.findById(id).map()`, `userRepository.findById()`
- [IdentityApiImpl.java:35](../../src/main/java/com/project/cybershop/identity/internal/service/impl/IdentityApiImpl.java#L35) `@Override public Optional<UserDto> getUserByEmail(String email)`
  - Lời gọi xuất hiện: `userRepository.findByEmailIgnoreCase(email).map()`, `userRepository.findByEmailIgnoreCase()`
- [IdentityApiImpl.java:40](../../src/main/java/com/project/cybershop/identity/internal/service/impl/IdentityApiImpl.java#L40) `@Override public boolean existsById(Integer id)`
  - Lời gọi xuất hiện: `userRepository.existsById()`

