package com.example.tismapps.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tismapps.NavigationStuff
import com.example.tismapps.R
import com.example.tismapps.navigateToScreen
import com.example.tismapps.ui.data.AppScreensRoutes

@Composable
fun HomeScreen(
    navStuff: NavigationStuff,
) {

    val cardModifier = Modifier

    LazyColumn(
        modifier = Modifier
            .background(Color(0xffe0e0eb))
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {

            Image(
                painter = painterResource(id = R.drawable.agro2),
                contentDescription = "AgriTech App",
            )
        }
        item {
            Text(
                text = stringResource(id = R.string.app_desc),
                modifier = Modifier.padding(16.dp, 16.dp),
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )
        }

        item {
            CustomMargin(16.dp)
            Divider(startIndent = 0.dp, thickness = 0.75.dp, color = Color(0xffb3daff))
            CustomMargin(16.dp)
        }

        item {
            UseCaseCard(
                modifier = cardModifier,
                imageId = R.drawable.watermelon,
                useCaseTitle = stringResource(R.string.pastai_usecase_title),
                useCaseDescription = stringResource(R.string.pastai_usecase_desc),
                onUseCaseButtonClicked = { navigateToScreen(navStuff, AppScreensRoutes.DetectorPytorch) }
            )
        }
        item {
            CustomMargin(16.dp)
            Divider(startIndent = 0.dp, thickness = 0.75.dp, color = Color(0xffb3daff))
            CustomMargin(16.dp)
        }
        item {
            UseCaseCard(
                modifier = cardModifier,
                imageId = R.drawable.whichflower,
                useCaseTitle = stringResource(R.string.whichFlower_usecase_title),
                useCaseDescription = stringResource(R.string.whichflower_usecase_desc),
                onUseCaseButtonClicked = { navigateToScreen(navStuff, AppScreensRoutes.DetectorTensorflow) }
            )
        }

    }

}

@Preview
@Composable
fun UseCaseCard(
    modifier: Modifier = Modifier,
    cardHeight: Dp = 450.dp,
    imageId: Int = R.drawable.ic_launcher_background,
    useCaseTitle: String = "App Title",
    useCaseDescription: String = "App Desc",
    onUseCaseButtonClicked: () -> Unit = { println("Button clicked!!!") }
) {
    Box(
        modifier
            .padding(horizontal = 16.dp)
            .background(Color(0xffb3ffb3))
            .height(cardHeight)
            .clickable(
                onClick = {},
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true)
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CustomMargin()
            Text(
                text = useCaseTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            CustomMargin()

            Divider(startIndent = 0.dp, thickness = 0.75.dp, color = Color.Gray)

            CustomMargin()
            Image(
                painter = painterResource(imageId),
                contentDescription = useCaseDescription,
                modifier = Modifier.size(325.dp, 300.dp),
                contentScale = ContentScale.FillBounds
            )
            CustomMargin()
            Text(
                text = useCaseDescription,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
            )
            CustomMargin()
            Button(
                onClick = onUseCaseButtonClicked,
                colors=ButtonDefaults.outlinedButtonColors(backgroundColor = Color(0xff009900)),
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text(
                    text = "<Start/>",
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
fun CustomMargin(marginHeight: Dp = 8.dp) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(marginHeight)
    )
}