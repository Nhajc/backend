package com.nhajc.server.user.service.impl;

import com.nhajc.server.user.UserAvatarStorage;
import com.nhajc.server.user.dto.UpdateProfileRequest;
import com.nhajc.server.user.dto.UserResponse;
import com.nhajc.server.user.entity.User;
import com.nhajc.server.user.mapper.UserResponseMapper;
import com.nhajc.server.user.repository.UserRepository;
import com.nhajc.server.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    private static final int MAX_AVATAR_BYTES = 5 * 1024 * 1024;

    private final UserRepository users;
    private final UserAvatarStorage avatars;
    private final UserResponseMapper responses;

    public UserServiceImpl(UserRepository users, UserAvatarStorage avatars,
                           UserResponseMapper responses) {
        this.users = users;
        this.avatars = avatars;
        this.responses = responses;
    }

    @Override
    @Transactional
    public UserResponse me(String subject) {
        return users.findById(userId(subject))
                .map(responses::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User no longer exists."));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String subject, UpdateProfileRequest request) {
        UUID userId = userId(subject);

        byte[] avatar = null;
        String contentType = null;
        if (request.avatar() != null) {
            if (request.avatar().getSize() > MAX_AVATAR_BYTES) {
                throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                        "Avatar must be at most 5 MB.");
            }
            try (var input = request.avatar().getInputStream()) {
                avatar = input.readNBytes(MAX_AVATAR_BYTES + 1);
            } catch (IOException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unable to read avatar.", exception);
            }
            if (avatar.length > MAX_AVATAR_BYTES) {
                throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                        "Avatar must be at most 5 MB.");
            }
            contentType = validateImage(avatar);
        }

        User user = users.findLockedById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User no longer exists."));
        avatars.replace(user, avatar, contentType);
        return responses.from(user);
    }

    private static UUID userId(String subject) {
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token.");
        }
    }

    private String validateImage(byte[] bytes) {
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidImage();
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("png")) {
                    throw invalidImage();
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) {
                    throw invalidImage();
                }
                // Decode a sampled image to validate the file with bounded pixel memory.
                ImageReadParam parameters = reader.getDefaultReadParam();
                parameters.setSourceSubsampling(
                        (int) Math.max(1L, (width + 1023L) / 1024),
                        (int) Math.max(1L, (height + 1023L) / 1024), 0, 0);
                reader.read(0, parameters);
                return "image/" + format;
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Avatar must be a non-empty, valid JPEG or PNG image.", exception);
        }
    }

    private ResponseStatusException invalidImage() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Avatar must be a non-empty, valid JPEG or PNG image.");
    }
}
