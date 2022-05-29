package com.company.photoapp.api.users.service;

import com.company.photoapp.api.users.data.UserEntity;
import com.company.photoapp.api.users.data.UserRepository;
import com.company.photoapp.api.users.shared.UserDto;
import com.company.photoapp.api.users.ui.model.AlbumResponseModel;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final RestTemplate restTemplate;
    private Environment env;

    @Autowired
    public void setEnv(Environment env) {
        this.env = env;
    }

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            BCryptPasswordEncoder bCryptPasswordEncoder,
            RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.restTemplate = restTemplate;
    }

    @Override
    public UserDto createUser(UserDto userDetails) {
        userDetails.setUserId(UUID.randomUUID().toString());
        userDetails.setEncryptedPassword(bCryptPasswordEncoder.encode(userDetails.getPassword()));
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserEntity userEntity = modelMapper.map(userDetails, UserEntity.class);
        userRepository.save(userEntity);
        UserDto returnValue = modelMapper.map(userEntity, UserDto.class);
        return returnValue;
    }

    @Override
    public UserDto getUserDetailsByEmail(String email) {
        UserEntity userEntity = userRepository.findByEmail(email);
        if (userEntity == null) {
            throw new UsernameNotFoundException(email);
        }
        return new ModelMapper().map(userEntity, UserDto.class);
    }

    @Override
    public UserDto getUserByUserId(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId);
        if (userEntity == null) {
            throw new UsernameNotFoundException("User not found");
        }
        UserDto userDto = new ModelMapper().map(userEntity, UserDto.class);

        String albumUrl = String.format(env.getProperty("albums.url"), userId);
        ResponseEntity<List<AlbumResponseModel>> albumsResponse = restTemplate.exchange(
                albumUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });
        List<AlbumResponseModel> albums = albumsResponse.getBody();

        userDto.setAlbums(albums);
        return userDto;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(username);
        if (userEntity == null) {
            throw new UsernameNotFoundException(username);
        }
        return new User(
                userEntity.getEmail(),
                userEntity.getEncryptedPassword(),
                true, true,
                true,
                true,
                new ArrayList<>());
    }
}
