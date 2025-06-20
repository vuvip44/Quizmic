package com.vuviet.userservice.config;

import com.vuviet.userservice.entity.Role;
import com.vuviet.userservice.entity.User;
import com.vuviet.userservice.repository.RoleRepository;
import com.vuviet.userservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataLoader implements CommandLineRunner {
    private final RoleRepository roleRepository;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoderl;

    public DataLoader(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoderl) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoderl = passwordEncoderl;
    }


    @Override
    public void run(String... args) throws Exception {
        loadRoles();
        loadDefaultAdmin();
    }

    private void loadRoles(){
        if(!roleRepository.existsByName("STUDENT")){
            Role studentRole=new Role();
            studentRole.setName("STUDENT");
            studentRole.setDescription("Student role - can take quizzes");
            roleRepository.save(studentRole);
            log.info("Created STUDENT role");
        }

        if(!roleRepository.existsByName("TEACHER")){
            Role teacherRole=new Role();
            teacherRole.setName("TEACHER");
            teacherRole.setDescription("Teacher role - can create and manage quizzes");
            roleRepository.save(teacherRole);
            log.info("Created TEACHER role");
        }

        if(!roleRepository.existsByName("ADMIN")){
            Role adminRole=new Role();
            adminRole.setName("ADMIN");
            adminRole.setDescription("Admin role - full system access");
            roleRepository.save(adminRole);
            log.info("Created ADMIN role");
        }
    }

    private void loadDefaultAdmin(){
        if(!userRepository.existsByUsername("admin")){
            Role adminRole=roleRepository.findByName("ADMIN")
                    .orElseThrow();
            User admin=new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoderl.encode("123456"));
            admin.setEmail("admin@gmail.com");
            admin.setFullName("System Administrator");
            admin.setRole(adminRole);
            admin.setIsActive(true);

            userRepository.save(admin);
            log.info("Create default admin user");
        }
    }
}
